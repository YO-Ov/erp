/*
 * HYUNWOO ERP 공통 프론트 헬퍼.
 *
 * 화면(Thymeleaf)은 껍데기만 그리고, 실제 데이터는 이 헬퍼로 REST API를 호출해 채운다.
 * 인증 토큰은 HttpOnly 쿠키(ACCESS_TOKEN)에 있어 fetch 가 자동으로 첨부하므로
 * 여기서 Authorization 헤더를 따로 붙일 필요가 없다.
 */
const ERP = (() => {

    /** 서버가 RFC 9457 ProblemDetail 로 내려준 에러를 감싼 예외. */
    class ApiError extends Error {
        constructor(status, problem) {
            super(problem?.detail || problem?.title || `요청 실패 (HTTP ${status})`);
            this.status = status;
            this.problem = problem || {};
            this.fieldErrors = problem?.fieldErrors || null; // {필드명: 메시지}
        }
    }

    /**
     * REST API 호출 래퍼.
     *  - body 를 주면 JSON 으로 직렬화해서 보낸다.
     *  - 401(미인증/쿠키만료)이면 로그인 화면으로 보낸다.
     *  - 2xx 가 아니면 ApiError 를 던진다(호출부에서 catch 해 flash 로 표시).
     */
    async function api(path, {method = 'GET', body} = {}) {
        const opts = {method, headers: {}};
        if (body !== undefined) {
            opts.headers['Content-Type'] = 'application/json';
            opts.body = JSON.stringify(body);
        }
        const resp = await fetch(path, opts);
        if (resp.status === 401) {
            window.location.href = '/login';
            throw new ApiError(401, {detail: '세션이 만료되었습니다.'});
        }
        const text = await resp.text();
        const data = text ? JSON.parse(text) : null;
        if (!resp.ok) {
            throw new ApiError(resp.status, data);
        }
        return data;
    }

    /** 숫자를 ₩ 통화 문자열로. */
    const won = n => (n == null || n === '' ? '' : '₩' + Number(n).toLocaleString('ko-KR'));
    /** 숫자에 천단위 콤마만. */
    const num = n => (n == null || n === '' ? '' : Number(n).toLocaleString('ko-KR'));

    /** 상태 뱃지 HTML — Tabler식 소프트 톤. color 는 primary/success/secondary/info/warning/danger/dark. */
    const badge = (text, color) => `<span class="badge badge-soft badge-soft-${color}">${text ?? ''}</span>`;

    /** XSS 방지용 간단 escape (사용자 입력을 innerHTML 로 넣을 때). */
    function esc(s) {
        return String(s ?? '').replace(/[&<>"']/g, c =>
            ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
    }

    /**
     * 페이지 상단 알림. id="flash" 컨테이너가 있으면 거기에, 없으면 alert().
     * type: success / danger / warning / info
     */
    function flash(message, type = 'danger') {
        const box = document.getElementById('flash');
        if (!box) {
            alert(message);
            return;
        }
        box.innerHTML =
            `<div class="alert alert-${type} alert-dismissible fade show" role="alert">` +
            `${esc(message)}<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>`;
        box.scrollIntoView({behavior: 'smooth', block: 'nearest'});
    }

    /** ApiError 를 flash 로 보여준다(필드 검증 오류는 풀어서). */
    function flashError(err) {
        if (err instanceof ApiError && err.fieldErrors) {
            const lines = Object.entries(err.fieldErrors).map(([f, m]) => `${f}: ${m}`).join(' / ');
            flash(`${err.message} (${lines})`, 'danger');
        } else {
            flash(err.message || String(err), 'danger');
        }
    }

    /** 빈 값을 제외한 쿼리스트링 생성. */
    function query(params) {
        const sp = new URLSearchParams();
        Object.entries(params).forEach(([k, v]) => {
            if (v !== '' && v !== null && v !== undefined) sp.append(k, v);
        });
        const s = sp.toString();
        return s ? '?' + s : '';
    }

    /** 현재 URL 의 쿼리 파라미터 읽기. */
    const param = name => new URLSearchParams(window.location.search).get(name);

    /**
     * 객체 배열 → CSV 문자열.
     * columns: [{label, value:(row)=>셀값}] — value 가 없으면 빈 칸.
     */
    function toCsv(rows, columns) {
        const cell = v => {
            const s = (v == null ? '' : String(v));
            return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
        };
        const head = columns.map(c => cell(c.label)).join(',');
        const body = rows.map(r => columns.map(c => cell(c.value ? c.value(r) : '')).join(','));
        return [head, ...body].join('\r\n');
    }

    /** 텍스트를 파일로 내려받기. UTF-8 BOM 을 붙여 엑셀에서 한글이 깨지지 않게 한다. */
    function download(filename, text, mime = 'text/csv;charset=utf-8') {
        const blob = new Blob(['﻿', text], {type: mime});
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    }

    /** 오늘 날짜 yyyymmdd — 다운로드 파일명 접미사용. */
    const today = () => new Date().toISOString().slice(0, 10).replace(/-/g, '');

    /**
     * 윈도우형 페이지네이션 + '전체 N건 중 X–Y' 범위 텍스트를 렌더한다.
     * 목록 화면 공통 — 각 화면은 load() 안에서 한 줄로 호출한다.
     *
     *   ERP.pager({page, size: SIZE, totalPages: d.totalPages, totalElements: d.totalElements,
     *              onGo: p => { page = p; load(); }});
     *
     * pagerId 엘리먼트(<ul>)에 페이지 버튼을, infoId 엘리먼트에 범위 텍스트를 채운다.
     */
    function pager({page, size, totalPages, totalElements, onGo, pagerId = 'pager', infoId = 'pageInfo'}) {
        const fmt = n => Number(n).toLocaleString('ko-KR');
        const info = infoId && document.getElementById(infoId);
        if (info) {
            info.textContent = totalElements > 0
                ? `전체 ${fmt(totalElements)}건 중 ${fmt(page * size + 1)}–${fmt(Math.min((page + 1) * size, totalElements))}`
                : '';
        }
        const el = document.getElementById(pagerId);
        if (!el) return;
        if (totalPages <= 1) { el.innerHTML = ''; el.onclick = null; return; }

        const mk = (label, p, {disabled = false, active = false} = {}) =>
            (disabled || active)
                ? `<li class="page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}"><span class="page-link">${label}</span></li>`
                : `<li class="page-item"><a class="page-link" href="#" data-go="${p}">${label}</a></li>`;
        const dots = '<li class="page-item disabled"><span class="page-link">…</span></li>';
        const win = 2; // 현재 페이지 좌우로 보여줄 개수
        const start = Math.max(0, page - win), end = Math.min(totalPages - 1, page + win);
        const out = [mk('‹', page - 1, {disabled: page === 0})];
        if (start > 0) { out.push(mk('1', 0)); if (start > 1) out.push(dots); }
        for (let i = start; i <= end; i++) out.push(mk(i + 1, i, {active: i === page}));
        if (end < totalPages - 1) { if (end < totalPages - 2) out.push(dots); out.push(mk(totalPages, totalPages - 1)); }
        out.push(mk('›', page + 1, {disabled: page === totalPages - 1}));
        el.innerHTML = out.join('');
        // 매 렌더마다 다시 그려지므로 addEventListener 누적 대신 onclick 1회 위임.
        el.onclick = e => {
            const a = e.target.closest('a[data-go]');
            if (!a) return;
            e.preventDefault();
            onGo(Number(a.dataset.go));
        };
    }

    function logout() {
        fetch('/api/auth/logout', {method: 'POST'})
            .finally(() => window.location.href = '/login');
    }

    return {api, ApiError, won, num, badge, esc, flash, flashError, query, param, logout, toCsv, download, today, pager, enhanceSelect};

    /* ────────────────────────────────────────────────────────────────
     * 검색 가능한 콤보박스 — 네이티브 <select> 를 숨기고 그 위에
     * "입력해서 거르는" UI 를 씌운다. 선택 시 원래 select 의 value 를
     * 그대로 세팅 + change 이벤트를 쏘므로, 기존 filters()/폼 submit/reset
     * 로직은 전혀 바꿀 필요가 없다(점진적 향상). 옵션은 API 로 나중에
     * 채워지므로 MutationObserver 로 도착을 감지해 다시 동기화한다.
     * ──────────────────────────────────────────────────────────────── */
    function enhanceSelect(select) {
        if (!select || select.dataset.comboReady) return;
        select.dataset.comboReady = '1';

        const wrap = document.createElement('div');
        wrap.className = 'erp-combo';
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'erp-combo-input form-control form-control-sm';
        input.setAttribute('role', 'combobox');
        input.setAttribute('autocomplete', 'off');
        input.setAttribute('aria-expanded', 'false');
        input.placeholder = select.dataset.placeholder || '선택 · 검색…';
        const caret = document.createElement('i');
        caret.className = 'bi bi-chevron-down erp-combo-caret';
        const menu = document.createElement('div');
        menu.className = 'erp-combo-menu';
        menu.setAttribute('role', 'listbox');

        select.parentNode.insertBefore(wrap, select);
        wrap.appendChild(select);
        wrap.appendChild(input);
        wrap.appendChild(caret);
        wrap.appendChild(menu);
        select.classList.add('erp-combo-native');
        // display:none 상태의 required select 은 네이티브 폼 검증에서 "not focusable" 로 제출 자체를 막는다.
        // 콤보로 바뀐 뒤엔 앱단(JS 수집·서버) 검증에 맡기므로 required 를 해제한다.
        if (select.required) select.required = false;

        let opts = [], filtered = [], activeIdx = -1, open = false;

        const currentLabel = () => {
            const o = select.options[select.selectedIndex];
            return o ? o.textContent : '';
        };
        function syncFromSelect() {
            opts = Array.from(select.options).map(o => ({value: o.value, label: o.textContent}));
            if (!open) input.value = currentLabel();
            if (open) renderMenu(input.value);
        }
        function renderMenu(q) {
            const query = (q || '').trim().toLowerCase();
            filtered = query ? opts.filter(o => o.label.toLowerCase().includes(query)) : opts.slice();
            menu.innerHTML = filtered.length
                ? filtered.map((o, i) =>
                    `<div class="erp-combo-opt${o.value === select.value ? ' selected' : ''}" data-i="${i}" role="option">${esc(o.label)}</div>`).join('')
                : `<div class="erp-combo-empty">검색 결과 없음</div>`;
            activeIdx = filtered.findIndex(o => o.value === select.value);
            highlight();
        }
        function highlight() {
            const items = menu.querySelectorAll('.erp-combo-opt');
            items.forEach((el, i) => el.classList.toggle('active', i === activeIdx));
            if (items[activeIdx]) items[activeIdx].scrollIntoView({block: 'nearest'});
        }
        function openMenu() {
            if (open) return;
            open = true;
            wrap.classList.add('open');
            input.setAttribute('aria-expanded', 'true');
            input.value = '';
            renderMenu('');
        }
        function closeMenu() {
            if (!open) return;
            open = false;
            wrap.classList.remove('open');
            input.setAttribute('aria-expanded', 'false');
            input.value = currentLabel();
        }
        function choose(o) {
            if (!o) { closeMenu(); return; }
            if (select.value !== o.value) {
                select.value = o.value;
                select.dispatchEvent(new Event('change', {bubbles: true}));
            }
            closeMenu();
        }

        input.addEventListener('focus', openMenu);
        input.addEventListener('click', openMenu);
        input.addEventListener('input', () => {
            if (!open) openMenu();
            renderMenu(input.value);
            activeIdx = filtered.length ? 0 : -1;
            highlight();
        });
        caret.addEventListener('mousedown', e => {
            e.preventDefault();
            if (open) closeMenu(); else { input.focus(); }
        });
        input.addEventListener('keydown', e => {
            if (e.key === 'ArrowDown')      { e.preventDefault(); if (!open) openMenu(); activeIdx = Math.min(activeIdx + 1, filtered.length - 1); highlight(); }
            else if (e.key === 'ArrowUp')   { e.preventDefault(); activeIdx = Math.max(activeIdx - 1, 0); highlight(); }
            else if (e.key === 'Enter')     { if (open) { e.preventDefault(); choose(filtered[activeIdx]); } }
            else if (e.key === 'Escape')    { if (open) { e.preventDefault(); closeMenu(); } }
        });
        menu.addEventListener('mousedown', e => {
            const el = e.target.closest('.erp-combo-opt');
            if (!el) return;
            e.preventDefault();
            choose(filtered[Number(el.dataset.i)]);
        });
        document.addEventListener('click', e => { if (!wrap.contains(e.target)) closeMenu(); });

        new MutationObserver(syncFromSelect).observe(select, {childList: true});
        const form = select.closest('form');
        if (form) form.addEventListener('reset', () => setTimeout(syncFromSelect, 0));
        syncFromSelect();
    }
})();

/* ══════════════════════════════════════════════════════════════════════
 * 목록 필터바 자동 향상 — 모든 화면 공통(파일별 수정 불필요).
 *   ① 모바일: 필터를 "조회조건" 토글로 접기(기본 닫힘) + 활성 조건 개수 배지
 *   ② 옵션이 많은 셀렉트(고객·영업담당 등)는 검색 가능한 콤보박스로 교체
 * ════════════════════════════════════════════════════════════════════ */
(function () {
    const SEARCH_THRESHOLD = 12; // 이 개수를 넘는 셀렉트만 검색 콤보로(짧은 enum 은 네이티브 유지)

    function considerSelect(select) {
        if (select.dataset.comboReady || select.dataset.noSearch !== undefined) return;
        if (select.options.length > SEARCH_THRESHOLD) { ERP.enhanceSelect(select); return; }
        // 옵션이 API 로 나중에 채워질 수 있으므로 임계치 초과 시점을 관찰
        const mo = new MutationObserver(() => {
            if (select.options.length > SEARCH_THRESHOLD) { mo.disconnect(); ERP.enhanceSelect(select); }
        });
        mo.observe(select, {childList: true});
    }

    function setupCollapsibleFilter(form) {
        if (form.dataset.collapsibleReady) return;
        form.dataset.collapsibleReady = '1';
        form.classList.add('is-collapsible');

        const bar = document.createElement('div');
        bar.className = 'filter-toggle d-md-none';
        bar.innerHTML =
            `<button type="button" class="filter-toggle-btn" aria-expanded="false">
                <span class="ft-label"><i class="bi bi-funnel"></i> 조회조건<span class="ft-count"></span></span>
                <i class="bi bi-chevron-down ft-caret"></i>
            </button>`;
        form.parentNode.insertBefore(bar, form);
        const btn = bar.querySelector('.filter-toggle-btn');

        const revealOverflow = () => { if (form.classList.contains('open')) form.style.overflow = 'visible'; };
        btn.addEventListener('click', () => {
            const isOpen = form.classList.toggle('open');
            btn.setAttribute('aria-expanded', isOpen);
            if (!isOpen) form.style.overflow = '';   // 접는 동안은 CSS(hidden) 로
            else setTimeout(revealOverflow, 320);    // 애니메이션 미동작 환경 폴백
        });
        // 펼침 애니메이션이 끝나면 콤보 드롭다운이 잘리지 않게 overflow 해제
        form.addEventListener('transitionend', e => {
            if (e.propertyName === 'max-height') revealOverflow();
        });

        const countBadge = bar.querySelector('.ft-count');
        function updateCount() {
            let n = 0;
            form.querySelectorAll('select, input').forEach(el => {
                if (['button', 'submit', 'reset'].includes(el.type)) return;
                if (el.classList.contains('erp-combo-input')) return; // 콤보 표시용 입력은 제외
                if (el.value) n++;
            });
            countBadge.textContent = n ? n : '';
            countBadge.classList.toggle('on', n > 0);
        }
        form.addEventListener('change', updateCount);
        form.addEventListener('submit', () => setTimeout(updateCount, 0));
        form.addEventListener('reset', () => setTimeout(updateCount, 0));
        setTimeout(updateCount, 500); // URL 파라미터 프리필·비동기 로딩 이후 반영
    }

    document.addEventListener('DOMContentLoaded', () => {
        // ① 목록 필터바: 모바일 접기 토글
        document.querySelectorAll('form.filter-bar').forEach(setupCollapsibleFilter);
        // ② 화면 내 모든 select 을 검색 콤보 후보로 — 목록 필터·작성 폼·모달 공통.
        //    옵션 12개 초과만 실제 향상(짧은 enum·페이지크기 등은 네이티브 유지, data-no-search 로 opt-out).
        document.querySelectorAll('select').forEach(considerSelect);
        // ③ 나중에 DOM 에 추가되는 select(예: 라인 아이템 행, 모달 필드)도 자동 향상.
        new MutationObserver(muts => {
            muts.forEach(m => m.addedNodes.forEach(node => {
                if (node.nodeType !== 1) return;                       // 엘리먼트만
                if (node.matches('select')) considerSelect(node);
                node.querySelectorAll('select').forEach(considerSelect);
            }));
        }).observe(document.body, {childList: true, subtree: true});
    });
})();
