requireAuth('ADMIN');

async function loadDashboard() {
    if (!document.getElementById('totalInv')) return;
    const [stats, inventory] = await Promise.all([api('/inventory/stats'), api('/inventory')]);
    document.getElementById('totalInv').textContent = stats.total;
    document.getElementById('availableInv').textContent = stats.available;
    document.getElementById('allocInv').textContent = stats.allocated;
    document.getElementById('lowStock').textContent = stats.lowStock;
    renderRows(document.getElementById('recentRows'), inventory.slice(0, 8), item => `
        <tr><td>${item.barcode}</td><td>${item.inventoryName}</td><td>${item.quantity}</td><td>${item.availableQuantity}</td><td><span class="status">${item.status}</span></td></tr>
    `);
}

async function loadInventory() {
    const tbody = document.getElementById('inventoryRows');
    if (!tbody) return;
    const inventory = await api('/inventory');
    renderRows(tbody, inventory, item => `
        <tr>
            <td>${item.barcode}</td><td>${item.inventoryName}</td><td>${item.category || ''}</td>
            <td>${item.quantity}</td><td>${item.availableQuantity}</td><td>${item.allocatedQuantity}</td><td>${item.status}</td>
            <td class="actions">
                <button type="button" data-edit="${item.id}"><i class="fa-solid fa-pen"></i></button>
                <button type="button" class="button danger" data-delete="${item.id}"><i class="fa-solid fa-trash"></i></button>
            </td>
        </tr>
    `);
    tbody.querySelectorAll('[data-edit]').forEach(btn => btn.addEventListener('click', () => {
        const item = inventory.find(row => row.id === btn.dataset.edit);
        document.getElementById('itemId').value = item.id;
        document.getElementById('barcode').value = item.barcode;
        document.getElementById('inventoryName').value = item.inventoryName;
        document.getElementById('category').value = item.category || '';
        document.getElementById('quantity').value = item.quantity;
        document.getElementById('serialNumber').value = item.serialNumber || '';
        document.getElementById('condition').value = item.condition || '';
        document.getElementById('description').value = item.description || '';
    }));
    tbody.querySelectorAll('[data-delete]').forEach(btn => btn.addEventListener('click', async () => {
        await api(`/inventory/${btn.dataset.delete}`, { method: 'DELETE' });
        await loadInventory();
    }));
}

async function setupInventoryForm() {
    const form = document.getElementById('inventoryForm');
    if (!form) return;
    document.getElementById('refreshBtn').addEventListener('click', loadInventory);
    document.getElementById('clearBtn').addEventListener('click', () => form.reset());
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('itemId').value;
        const body = {
            barcode: document.getElementById('barcode').value.trim(),
            inventoryName: document.getElementById('inventoryName').value.trim(),
            category: document.getElementById('category').value.trim(),
            quantity: Number(document.getElementById('quantity').value),
            serialNumber: document.getElementById('serialNumber').value.trim(),
            condition: document.getElementById('condition').value.trim(),
            description: document.getElementById('description').value.trim()
        };
        await api(id ? `/inventory/${id}` : '/inventory', { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) });
        form.reset();
        document.getElementById('itemId').value = '';
        await loadInventory();
    });
}

async function setupAllocation() {
    const form = document.getElementById('allocationForm');
    if (!form) return;
    const [inventory, departments, allocations] = await Promise.all([api('/inventory'), api('/departments'), api('/allocations')]);
    document.getElementById('inventoryId').innerHTML = inventory.map(item => `<option value="${item.id}">${item.inventoryName} (${item.availableQuantity} available)</option>`).join('');
    document.getElementById('departmentId').innerHTML = departments.map(dep => `<option value="${dep.id}">${dep.departmentName}</option>`).join('');
    renderRows(document.getElementById('allocationRows'), allocations, item => `
        <tr><td>${item.inventoryName}</td><td>${item.departmentName}</td><td>${item.quantity}</td><td>${item.status}</td><td>${new Date(item.allocatedAt).toLocaleString()}</td></tr>
    `);
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await api('/allocations', {
            method: 'POST',
            body: JSON.stringify({
                inventoryId: document.getElementById('inventoryId').value,
                departmentId: document.getElementById('departmentId').value,
                quantity: Number(document.getElementById('allocQuantity').value)
            })
        });
        window.location.reload();
    });
}

loadDashboard().catch(console.error);
setupInventoryForm().then(loadInventory).catch(console.error);
setupAllocation().catch(console.error);

// Add this function to admin.js
async function deleteDepartment(deptId) {
    try {
        await api(`/departments/${deptId}`, { method: 'DELETE' });
        return true;
    } catch (err) {
        console.error('Delete failed:', err);
        throw err;
    }
}