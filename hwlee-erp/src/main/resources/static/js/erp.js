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

    return {api, ApiError, won, num, badge, esc, flash, flashError, query, param, logout, toCsv, download, today, pager};
})();
