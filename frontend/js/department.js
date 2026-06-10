requireAuth('DEPARTMENT');

async function loadDepartmentInventory() {
    const tbody = document.getElementById('departmentRows');
    if (!tbody) return;
    const departmentId = localStorage.getItem('departmentId');
    if (!departmentId) {
        tbody.innerHTML = '<tr><td colspan="4">No department is linked to this account.</td></tr>';
        return;
    }
    const rows = await api(`/allocations/department/${departmentId}`);
    renderRows(tbody, rows, item => `
        <tr><td>${item.inventoryName}</td><td>${item.quantity}</td><td>${item.status}</td><td>${new Date(item.allocatedAt).toLocaleString()}</td></tr>
    `);
}

loadDepartmentInventory().catch(console.error);
