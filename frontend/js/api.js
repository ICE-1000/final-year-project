const API_BASE = (() => {
    const trim = (s) => s ? s.replace(/\/+$/g, '') : s;
    const configuredBase = localStorage.getItem('apiBase');
    if (configuredBase) return trim(configuredBase);
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        return 'http://localhost:8080/api';
    }
    return 'https://final-year-project-oref.onrender.com/api';
})();

function token() {
    return localStorage.getItem('token');
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('departmentId');
    window.location.href = '../index.html';
}

async function api(path, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };
    if (token()) headers.Authorization = `Bearer ${token()}`;
    const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
    if (!res.ok) {
        let message = `HTTP ${res.status}`;
        try {
            const body = await res.json();
            message = body.message || message;
        } catch (err) {
            if (res.statusText) message = `${message} ${res.statusText}`;
        }
        throw new Error(message);
    }
    if (res.status === 204) return null;
    return res.json();
}

function requireAuth(role) {
    if (!token()) window.location.href = '../index.html';
    const currentRole = localStorage.getItem('role');
    if (role && currentRole !== role) window.location.href = '../index.html';
    const btn = document.getElementById('logoutBtn');
    if (btn) btn.addEventListener('click', logout);
}

function escapeHtml(value) {
    if (!value) return '';
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function renderRows(tbody, rows, renderer, colspan = 8) {
    tbody.innerHTML = rows.length ? rows.map(renderer).join('') : `<tr><td colspan="${colspan}">No records found.</td></tr>`;
}

async function createRequest(payload) {
    return api('/requests', {
        method: 'POST',
        body: JSON.stringify(payload)
    });
}

async function fetchMyRequests() {
    return api('/requests/me');
}

async function fetchAllRequests(status) {
    const query = status ? `?status=${encodeURIComponent(status)}` : '';
    return api(`/requests${query}`);
}

async function updateRequestStatus(id, status, rejectionReason) {
    return api(`/requests/${id}/status`, {
        method: 'PUT',
        body: JSON.stringify({ status, rejectionReason })
    });
}

// Fetches a file (PDF/PNG/etc.) with the auth header attached, then triggers a
// save-as via a blob URL - used for report downloads and individual/bulk barcode
// image downloads. A plain <a download> tag doesn't reliably force a save for an
// authenticated or cross-origin resource, so every download in this app goes through
// this helper instead.
async function downloadFile(url, filename) {
    const res = await fetch(url, { headers: token() ? { Authorization: `Bearer ${token()}` } : {} });
    if (!res.ok) throw new Error('Download failed');
    const blob = await res.blob();
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(objectUrl);
}

/* Responsive sidebar & touch improvements */
function initResponsiveSidebar() {
    if (typeof document === 'undefined') return;
    const SIDEBAR_BREAKPOINT = 900;
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return;

    let overlay = document.querySelector('.sidebar-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        document.body.appendChild(overlay);
    }

    let btn = document.querySelector('.mobile-menu-btn');
    if (!btn) {
        btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'mobile-menu-btn';
        btn.setAttribute('aria-label', 'Open menu');
        btn.innerHTML = '\u2630';
        document.body.appendChild(btn);
    }

    function openSidebar() {
        sidebar.classList.add('open');
        sidebar.classList.remove('collapsed');
        overlay.classList.add('visible');
        btn.setAttribute('aria-expanded', 'true');
    }
    function closeSidebar() {
        sidebar.classList.remove('open');
        sidebar.classList.add('collapsed');
        overlay.classList.remove('visible');
        btn.setAttribute('aria-expanded', 'false');
    }

    btn.addEventListener('click', () => {
        if (sidebar.classList.contains('open')) closeSidebar(); else openSidebar();
    });

    overlay.addEventListener('click', () => closeSidebar());

    function attachNavLinkHandlers() {
        const links = sidebar.querySelectorAll('a');
        links.forEach(l => l.addEventListener('click', () => {
            if (window.innerWidth <= SIDEBAR_BREAKPOINT) closeSidebar();
        }));
    }
    attachNavLinkHandlers();

    document.addEventListener('keydown', (ev) => {
        if (ev.key === 'Escape' || ev.key === 'Esc') {
            if (sidebar.classList.contains('open')) closeSidebar();
        }
    });

    function updateTouchTargets() {
        const buttons = Array.from(document.querySelectorAll('button, .button'));
        if (window.innerWidth <= SIDEBAR_BREAKPOINT) {
            buttons.forEach(b => b.classList.add('touch-target'));
        } else {
            buttons.forEach(b => b.classList.remove('touch-target'));
        }
    }

    let resizeTimer;
    function onResize() {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
            updateTouchTargets();
            if (window.innerWidth > SIDEBAR_BREAKPOINT) {
                sidebar.classList.remove('open');
                sidebar.classList.remove('collapsed');
                overlay.classList.remove('visible');
                btn.style.display = '';
            } else {
                if (!sidebar.classList.contains('open')) sidebar.classList.add('collapsed');
            }
        }, 120);
    }

    window.addEventListener('resize', onResize);

    if (window.innerWidth <= SIDEBAR_BREAKPOINT) {
        sidebar.classList.add('collapsed');
        btn.style.display = 'inline-flex';
    } else {
        sidebar.classList.remove('collapsed');
        btn.style.display = '';
    }
    updateTouchTargets();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initResponsiveSidebar);
} else {
    initResponsiveSidebar();
}
