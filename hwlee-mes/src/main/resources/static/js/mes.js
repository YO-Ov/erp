/* MES 공통 프론트 유틸 — REST 호출 래퍼 + 포맷. (MES 는 인증 없음) */
const MES = (function () {
    async function api(path, {method = 'GET', body} = {}) {
        const opt = {method, headers: {}};
        if (body !== undefined) {
            opt.headers['Content-Type'] = 'application/json';
            opt.body = JSON.stringify(body);
        }
        const res = await fetch(path, opt);
        const text = await res.text();
        const data = text ? JSON.parse(text) : null;
        if (!res.ok) {
            const err = new Error((data && data.message) || ('요청 실패 (HTTP ' + res.status + ')'));
            err.data = data;
            throw err;
        }
        return data;
    }

    const num = n => (n == null || n === '' ? '' : Number(n).toLocaleString('ko-KR'));
    const esc = s => String(s ?? '').replace(/[&<>"']/g,
        c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
    const badge = (text, color) => `<span class="badge bg-${color}">${esc(text)}</span>`;

    /** 작업지시 상태 → 뱃지 색. */
    const statusColor = s => ({
        RECEIVED: 'secondary', IN_PROGRESS: 'primary', PAUSED: 'warning',
        COMPLETED: 'success', CANCELLED: 'dark'
    }[s] || 'secondary');

    function flash(message, type = 'danger') {
        const box = document.getElementById('flash');
        if (!box) { alert(message); return; }
        box.innerHTML = `<div class="alert alert-${type} alert-dismissible" role="alert">`
            + `${esc(message)}<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>`;
    }

    return {api, num, esc, badge, statusColor, flash};
})();
