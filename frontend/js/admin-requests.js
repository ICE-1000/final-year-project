// admin-requests.js
requireAuth('ADMIN');

function formatMonth(value) {
    if (!value) return '';
    const date = new Date(value);
    return date.toLocaleDateString(undefined, { year: 'numeric', month: 'long' });
}

function formatDate(value) {
    if (!value) return '';
    const date = new Date(value);
    return date.toLocaleDateString();
}

// Load all requests (with optional status filter)
async function loadAdminRequests() {
    const tbody = document.getElementById('requestsRows');
    if (!tbody) return;
    const statusFilter = document.getElementById('statusFilter');
    const status = statusFilter ? statusFilter.value : '';
    let requests;
    try {
        tbody.innerHTML = '<tr><td colspan="8">Loading requests...</td></tr>';
        requests = await fetchAllRequests(status);
        renderRows(tbody, requests, request => `
            <tr>
                <td>${escapeHtml(request.departmentName)}</td>
                <td>${escapeHtml(request.itemName)}</td>
                <td>${request.quantity}</td>
                <td>${formatMonth(request.neededBy)}</td>
                <td><span class="status">${escapeHtml(request.status)}</span></td>
                <td>${escapeHtml(request.rejectionReason || '')}</td>
                <td>${formatDate(request.createdAt)}</td>
                <td class="actions">
                    ${request.status === 'PENDING' ? `
                    <button type="button" data-action="approve" data-id="${request.id}">Approve</button>
                    <button type="button" data-action="reject" data-id="${request.id}">Reject</button>
                    ` : ''}
                </td>
            </tr>
        `, 8);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="8">${escapeHtml(error.message || 'Failed to load requests.')}</td></tr>`;
        console.error(error);
        return;
    }

    // Attach event listeners for approve/reject buttons
    tbody.querySelectorAll('button[data-action]').forEach(button => {
        button.addEventListener('click', async () => {
            const id = button.dataset.id;
            const action = button.dataset.action;
            try {
                button.disabled = true;
                if (action === 'approve') {
                    await updateRequestStatus(id, 'APPROVED');
                } else {
                    const reason = prompt('Enter rejection reason:');
                    if (!reason || !reason.trim()) return;
                    await updateRequestStatus(id, 'REJECTED', reason.trim());
                }
                // Refresh the list after action
                await loadAdminRequests();
            } catch (error) {
                alert(error.message || 'Action failed.');
                console.error(error);
                button.disabled = false;
            }
        });
    });
}

// Initialise admin request page
async function initAdminRequests() {
    const filterBtn = document.getElementById('filterBtn');
    if (filterBtn) {
        filterBtn.addEventListener('click', loadAdminRequests);
    }
    await loadAdminRequests();
}

initAdminRequests().catch(console.error);
