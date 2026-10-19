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

// Converts a <input type="month"> value ("YYYY-MM") into an ISO date string safe to
// send as neededBy (backend requires @FutureOrPresent). Using "-01" unconditionally
// would make the CURRENT month fail validation on any day after the 1st, since that
// date would already be in the past - so when the selected month is the current
// month, use today's actual date instead.
function monthInputToNeededByDate(monthValue) {
    const [year, month] = monthValue.split('-').map(Number);
    const today = new Date();
    const isCurrentMonth = year === today.getFullYear() && month === (today.getMonth() + 1);
    if (isCurrentMonth) {
        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');
        return `${yyyy}-${mm}-${dd}`;
    }
    return `${monthValue}-01`;
}

async function loadCategoryOptions() {
    const select = document.getElementById('categorySelect');
    if (!select) return;
    const categories = await api('/categories');
    const placeholder = select.querySelector('option[value=""]');
    select.innerHTML = '';
    select.appendChild(placeholder || Object.assign(document.createElement('option'), { value: '', textContent: 'Select a category...' }));
    categories.forEach(cat => {
        const opt = document.createElement('option');
        opt.value = cat.id;
        opt.textContent = cat.name;
        select.appendChild(opt);
    });
}

async function showAvailabilityForCategory(categoryId) {
    const panel = document.getElementById('availabilityPanel');
    const tbody = document.getElementById('availabilityRows');
    if (!panel || !tbody) return;

    if (!categoryId) {
        panel.style.display = 'none';
        return;
    }

    panel.style.display = 'block';
    tbody.innerHTML = '<tr><td colspan="4">Loading...</td></tr>';
    try {
        const items = await api(`/inventory/category/${categoryId}`);
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="4">Nothing currently in stock for this category.</td></tr>';
            return;
        }
        tbody.innerHTML = items.map(item => `
            <tr>
                <td>${escapeHtml(item.inventoryName)}</td>
                <td>${item.availableQuantity}</td>
                <td>${escapeHtml(item.condition || '-')}</td>
                <td>${escapeHtml(item.description || '-')}</td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4">${escapeHtml(err.message || 'Failed to load availability.')}</td></tr>`;
    }
}

function setupCategoryBrowsing() {
    const select = document.getElementById('categorySelect');
    if (!select) return;
    loadCategoryOptions().catch(console.error);
    select.addEventListener('change', () => showAvailabilityForCategory(select.value));
}

async function submitRequestForm() {
    const form = document.getElementById('requestForm');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const categorySelect = document.getElementById('categorySelect');
        const categoryId = categorySelect ? categorySelect.value : '';
        const itemName = document.getElementById('itemName').value.trim();
        const quantity = Number(document.getElementById('quantity').value);
        const neededByMonth = document.getElementById('neededBy').value;
        const description = document.getElementById('description').value.trim();
        const message = document.getElementById('message');
        const submitButton = form.querySelector('button[type="submit"]');

        if (!categoryId) {
            if (message) message.textContent = 'Please select a category.';
            return;
        }
        if (!itemName || !quantity || !neededByMonth) {
            if (message) message.textContent = 'Please complete all required fields.';
            return;
        }

        const neededBy = monthInputToNeededByDate(neededByMonth);
        try {
            if (message) message.textContent = 'Submitting request...';
            if (submitButton) submitButton.disabled = true;
            await createRequest({ categoryId, itemName, quantity, neededBy, description });
            if (message) message.textContent = 'Request submitted successfully.';
            form.reset();
            const panel = document.getElementById('availabilityPanel');
            if (panel) panel.style.display = 'none';
        } catch (error) {
            if (message) message.textContent = error.message || 'Failed to submit request.';
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
        renderRows(tbody, [], () => '', 7);
        const requests = await fetchMyRequests();
        renderRows(tbody, requests, request => `
            <tr>
                <td>${escapeHtml(request.categoryName || '-')}</td>
                <td>${escapeHtml(request.itemName)}</td>
                <td>${request.quantity}</td>
                <td>${formatMonth(request.neededBy)}</td>
                <td><span class="status">${escapeHtml(request.status)}</span></td>
                <td>${escapeHtml(request.rejectionReason || '')}</td>
                <td>${formatDate(request.createdAt)}</td>
            </tr>
        `, 7);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7">${escapeHtml(error.message || 'Failed to load requests.')}</td></tr>`;
        console.error(error);
    }
}

async function initDepartmentRequests() {
    setupCategoryBrowsing();
    await submitRequestForm();
    await loadMyRequests();
}

initDepartmentRequests().catch(console.error);
