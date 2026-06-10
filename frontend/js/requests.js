requireAuth();

function formatDate(value) {
    if (!value) return '';
    const date = new Date(value);
    return date.toLocaleDateString();
}

function formatMonth(value) {
    if (!value) return '';
    const date = new Date(value);
    return date.toLocaleDateString(undefined, { year: 'numeric', month: 'long' });
}

async function submitRequestForm() {
    const form = document.getElementById('requestForm');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const itemName = document.getElementById('itemName').value.trim();
        const quantity = Number(document.getElementById('quantity').value);
        const neededByMonth = document.getElementById('neededBy').value;
        const description = document.getElementById('description').value.trim();
        const message = document.getElementById('message');
        const submitButton = form.querySelector('button[type="submit"]');

        if (!itemName || !quantity || !neededByMonth) {
            if (message) message.textContent = 'Please complete all required fields.';
            return;
        }

        const neededBy = `${neededByMonth}-01`;
        try {
            if (message) message.textContent = 'Submitting request...';
            if (submitButton) submitButton.disabled = true;
            await createRequest({ itemName, quantity, neededBy, description });
            if (message) message.textContent = 'Request submitted successfully.';
            form.reset();
        } catch (error) {
            if (message) message.textContent = error.message;
            console.error(error);
        } finally {
            if (submitButton) submitButton.disabled = false;
        }
    });
}

async function loadMyRequests() {
    const tbody = document.getElementById('requestsRows');
    if (!tbody) return;
    try {
        renderRows(tbody, [], () => '', 6);
        const requests = await fetchMyRequests();
        renderRows(tbody, requests, request => `
            <tr>
                <td>${escapeHtml(request.itemName)}</td>
                <td>${request.quantity}</td>
                <td>${formatMonth(request.neededBy)}</td>
                <td><span class="status">${escapeHtml(request.status)}</span></td>
                <td>${escapeHtml(request.rejectionReason || '')}</td>
                <td>${formatDate(request.createdAt)}</td>
            </tr>
        `, 6);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="6">${escapeHtml(error.message)}</td></tr>`;
        console.error(error);
    }
}

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
        tbody.innerHTML = `<tr><td colspan="8">${escapeHtml(error.message)}</td></tr>`;
        console.error(error);
        return;
    }
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
            } catch (error) {
                alert(error.message);
                console.error(error);
            } finally {
                button.disabled = false;
            }
            await loadAdminRequests();
        });
    });
}

async function initRequestPages() {
    await submitRequestForm();
    await loadMyRequests();
    const filterBtn = document.getElementById('filterBtn');
    if (filterBtn) {
        filterBtn.addEventListener('click', loadAdminRequests);
    }
    await loadAdminRequests();
}

initRequestPages().catch(console.error);
