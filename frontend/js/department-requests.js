// department-requests.js
requireAuth('DEPARTMENT');

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

// Submit a new inventory request (for request-inventory.html)
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
            if (message) message.textContent = error.message || 'Failed to submit request.';
            console.error(error);
        } finally {
            if (submitButton) submitButton.disabled = false;
        }
    });
}

// Load the department's own requests (for my-requests.html)
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
        tbody.innerHTML = `<tr><td colspan="6">${escapeHtml(error.message || 'Failed to load requests.')}</td></tr>`;
        console.error(error);
    }
}

// Initialise all department request pages
async function initDepartmentRequests() {
    await submitRequestForm();
    await loadMyRequests();
}

initDepartmentRequests().catch(console.error);
