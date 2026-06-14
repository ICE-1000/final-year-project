const API_BASE = (() => {
    const trim = (s) => s ? s.replace(/\/+$/g, '') : s;
    const configuredBase = localStorage.getItem('apiBase');
    if (configuredBase) return trim(configuredBase);
    // Keep localhost for local development. For non-local use the deployed backend URL.
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        return 'http://localhost:8080/api';
    }
    // Deployed backend (ensure no trailing slash and include /api)
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

/* Responsive sidebar & touch improvements
   - Creates hamburger button <=900px
   - Toggles sidebar open/close and overlay
   - Closes sidebar on nav link click, Escape key, or overlay click
   - Resets state on resize
   - Adds touch-target class to buttons on small screens
*/
function initResponsiveSidebar() {
    if (typeof document === 'undefined') return;
    const SIDEBAR_BREAKPOINT = 900;
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return; // nothing to do if no sidebar on page

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
        btn.innerHTML = '\u2630'; // simple hamburger ☰
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

    btn.addEventListener('click', (e) => {
        if (sidebar.classList.contains('open')) closeSidebar(); else openSidebar();
    });

    overlay.addEventListener('click', () => closeSidebar());

    // Close when a sidebar nav link is clicked
    function attachNavLinkHandlers() {
        const links = sidebar.querySelectorAll('a');
        links.forEach(l => l.addEventListener('click', () => {
            if (window.innerWidth <= SIDEBAR_BREAKPOINT) closeSidebar();
        }));
    }
    attachNavLinkHandlers();

    // Close on Escape
    document.addEventListener('keydown', (ev) => {
        if (ev.key === 'Escape' || ev.key === 'Esc') {
            if (sidebar.classList.contains('open')) closeSidebar();
        }
    });

    // Improve touch targets for buttons on small screens
    function updateTouchTargets() {
        const buttons = Array.from(document.querySelectorAll('button, .button'));
        if (window.innerWidth <= SIDEBAR_BREAKPOINT) {
            buttons.forEach(b => b.classList.add('touch-target'));
        } else {
            buttons.forEach(b => b.classList.remove('touch-target'));
        }
    }

    // Reset on resize to keep desktop layout stable
    let resizeTimer;
    function onResize() {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
            updateTouchTargets();
            if (window.innerWidth > SIDEBAR_BREAKPOINT) {
                // ensure sidebar visible and overlay hidden on desktop
                sidebar.classList.remove('open');
                sidebar.classList.remove('collapsed');
                overlay.classList.remove('visible');
                btn.style.display = '';
            } else {
                // ensure collapsed by default on mobile
                if (!sidebar.classList.contains('open')) sidebar.classList.add('collapsed');
            }
        }, 120);
    }

    window.addEventListener('resize', onResize);

    // initial state
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
