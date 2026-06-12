const API_BASE = (() => {
    const configuredBase = localStorage.getItem('apiBase');
    if (configuredBase) return configuredBase;
    // Keep localhost for local development. For non-local use the deployed backend URL.
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
