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

    function logout() {
        fetch('/api/auth/logout', {method: 'POST'})
            .finally(() => window.location.href = '/login');
    }

    return {api, ApiError, won, num, badge, esc, flash, flashError, query, param, logout};
})();
