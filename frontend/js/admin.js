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

async function loadCategoryOptions() {
    const select = document.getElementById('categorySelect');
    if (!select) return;
    const categories = await api('/categories');
    const placeholder = select.querySelector('option[value=""]');
    select.innerHTML = '';
    if (placeholder) select.appendChild(placeholder);
    else select.innerHTML = '<option value="">Select a category...</option>';
    categories.forEach(cat => {
        const opt = document.createElement('option');
        opt.value = cat.id;
        opt.textContent = cat.name;
        select.appendChild(opt);
    });
}

async function loadInventory() {
    const tbody = document.getElementById('inventoryRows');
    if (!tbody) return;
    const inventory = await api('/inventory');
    renderRows(tbody, inventory, item => `
        <tr>
            <td>${item.barcode}</td><td>${item.inventoryName}</td><td>${item.brand || '-'}</td><td>${item.specification || '-'}</td>
            <td>${item.categoryName || '-'}</td>
            <td>${item.quantity}</td><td>${item.availableQuantity}</td><td>${item.allocatedQuantity}</td><td>${item.status}</td>
            <td class="actions">
                <button type="button" data-edit="${item.id}" title="Edit details"><i class="fa-solid fa-pen"></i></button>
                <button type="button" class="button secondary" data-view-units="${item.id}" title="View / print unit barcodes"><i class="fa-solid fa-barcode"></i></button>
                <button type="button" class="button secondary" data-view-batches="${item.id}" title="View registration history"><i class="fa-solid fa-clock-rotate-left"></i></button>
                <button type="button" class="button danger" data-delete="${item.id}" title="Delete"><i class="fa-solid fa-trash"></i></button>
            </td>
        </tr>
    `, 10);

    const quantityField = document.getElementById('quantity');
    const quantityLabel = document.getElementById('quantityLabel');
    const quantityHelp = document.getElementById('quantityHelp');

    tbody.querySelectorAll('[data-edit]').forEach(btn => btn.addEventListener('click', () => {
        const item = inventory.find(row => row.id === btn.dataset.edit);
        document.getElementById('itemId').value = item.id;
        const barcodeInput = document.getElementById('barcode');
        barcodeInput.value = item.barcode;
        barcodeInput.readOnly = true;
        const categorySelect = document.getElementById('categorySelect');
        if (categorySelect) categorySelect.value = item.categoryId || '';
        document.getElementById('inventoryName').value = item.inventoryName;
        document.getElementById('brand').value = item.brand || '';
        document.getElementById('specification').value = item.specification || '';
        document.getElementById('serialNumber').value = item.serialNumber || '';
        document.getElementById('condition').value = item.condition || '';
        document.getElementById('description').value = item.description || '';
        // Quantity can't be changed here - only by registering more stock (leave the
        // ID field blank and resubmit with the same name/brand/specification, which
        // merges into this same item and mints new unit barcodes).
        quantityField.value = item.quantity;
        quantityField.disabled = true;
        quantityField.required = false;
        if (quantityLabel) quantityLabel.textContent = 'Current Quantity (read-only)';
        if (quantityHelp) quantityHelp.textContent = 'To add more stock, use "Clear" above and register the same name + brand + specification again.';
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }));
    tbody.querySelectorAll('[data-view-units]').forEach(btn => btn.addEventListener('click', async () => {
        const item = inventory.find(row => row.id === btn.dataset.viewUnits);
        try {
            const units = await api(`/inventory/${item.id}/units`);
            openUnitsModal(
                `Units: ${item.inventoryName}${item.brand ? ' (' + item.brand + ')' : ''}`,
                units,
                {
                    pdfUrl: `${API_BASE}/reports/inventory-barcodes.pdf?inventoryId=${item.id}`,
                    zipUrl: `${API_BASE}/reports/inventory-barcodes.zip?inventoryId=${item.id}`
                }
            );
        } catch (err) {
            alert(err.message || 'Failed to load units');
        }
    }));
    tbody.querySelectorAll('[data-view-batches]').forEach(btn => btn.addEventListener('click', async () => {
        const item = inventory.find(row => row.id === btn.dataset.viewBatches);
        try {
            const batches = await api(`/inventory/${item.id}/batches`);
            openBatchesModal(`Registration History: ${item.inventoryName}${item.brand ? ' (' + item.brand + ')' : ''}`, batches);
        } catch (err) {
            alert(err.message || 'Failed to load registration history');
        }
    }));
    tbody.querySelectorAll('[data-delete]').forEach(btn => btn.addEventListener('click', async () => {
        if (!confirm('Delete this item? This only works if it has no active allocation history tied to units.')) return;
        await api(`/inventory/${btn.dataset.delete}`, { method: 'DELETE' });
        await loadInventory();
    }));
}

function resetInventoryForm(form) {
    form.reset();
    document.getElementById('itemId').value = '';
    document.getElementById('barcode').readOnly = false;
    const quantityField = document.getElementById('quantity');
    quantityField.disabled = false;
    quantityField.required = true;
    const quantityLabel = document.getElementById('quantityLabel');
    const quantityHelp = document.getElementById('quantityHelp');
    if (quantityLabel) quantityLabel.textContent = 'Quantity to Register';
    if (quantityHelp) quantityHelp.textContent = 'Registering the same name + brand + specification again in this category adds to this item\'s existing stock instead of creating a duplicate - each unit gets its own individually numbered barcode, continuing from where the last batch left off.';
}

async function setupInventoryForm() {
    const form = document.getElementById('inventoryForm');
    if (!form) return;
    await loadCategoryOptions();
    document.getElementById('refreshBtn').addEventListener('click', loadInventory);
    document.getElementById('clearBtn').addEventListener('click', () => resetInventoryForm(form));
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('itemId').value;
        const categorySelect = document.getElementById('categorySelect');
        if (categorySelect && !categorySelect.value) {
            alert('Please select a category.');
            return;
        }
        const body = {
            barcode: document.getElementById('barcode').value.trim(),
            categoryId: categorySelect ? categorySelect.value : null,
            inventoryName: document.getElementById('inventoryName').value.trim(),
            brand: document.getElementById('brand').value.trim(),
            specification: document.getElementById('specification').value.trim(),
            serialNumber: document.getElementById('serialNumber').value.trim(),
            condition: document.getElementById('condition').value.trim(),
            description: document.getElementById('description').value.trim(),
            quantity: Number(document.getElementById('quantity').value || 0)
        };
        try {
            await api(id ? `/inventory/${id}` : '/inventory', { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) });
            resetInventoryForm(form);
            await loadInventory();
        } catch (err) {
            alert(err.message || 'Failed to save item');
        }
    });
}

async function setupAllocation() {
    const form = document.getElementById('allocationForm');
    if (!form) return;
    const [inventory, departments, allocations] = await Promise.all([api('/inventory'), api('/departments'), api('/allocations')]);
    document.getElementById('inventoryId').innerHTML = inventory.map(item => `<option value="${item.id}">${item.inventoryName}${item.brand ? ' - ' + item.brand : ''} (${item.availableQuantity} available)</option>`).join('');
    document.getElementById('departmentId').innerHTML = departments.map(dep => `<option value="${dep.id}">${dep.departmentName}</option>`).join('');
    renderRows(document.getElementById('allocationRows'), allocations, item => `
        <tr>
            <td>${item.inventoryName}</td><td>${item.departmentName}</td><td>${item.quantity}</td><td>${item.status}</td>
            <td>${new Date(item.allocatedAt).toLocaleString()}</td>
            <td><button type="button" class="button secondary" data-expand-alloc="${item.id}" data-alloc-label="${escapeHtml(item.inventoryName + ' \u2192 ' + item.departmentName)}"><i class="fa-solid fa-barcode"></i> View Barcodes</button></td>
        </tr>
    `, 6);

    document.querySelectorAll('[data-expand-alloc]').forEach(btn => btn.addEventListener('click', async () => {
        const id = btn.getAttribute('data-expand-alloc');
        try {
            const units = await api(`/allocations/${id}/units`);
            openUnitsModal(btn.getAttribute('data-alloc-label'), units, {
                pdfUrl: `${API_BASE}/reports/allocation-barcodes.pdf?allocationId=${id}`,
                zipUrl: `${API_BASE}/reports/allocation-barcodes.zip?allocationId=${id}`
            });
        } catch (err) {
            alert(err.message || 'Failed to load allocation units');
        }
    }));

    const downloadAllBtn = document.getElementById('downloadAllBarcodesBtn');
    if (downloadAllBtn) {
        downloadAllBtn.addEventListener('click', async () => {
            downloadAllBtn.disabled = true;
            try {
                await downloadFile(`${API_BASE}/reports/allocation-barcodes.pdf`, 'all-allocation-barcodes.pdf');
            } catch (err) {
                alert(err.message || 'Failed to download');
            } finally {
                downloadAllBtn.disabled = false;
            }
        });
    }
    const downloadAllZipBtn = document.getElementById('downloadAllBarcodesZipBtn');
    if (downloadAllZipBtn) {
        downloadAllZipBtn.addEventListener('click', async () => {
            downloadAllZipBtn.disabled = true;
            try {
                await downloadFile(`${API_BASE}/reports/allocation-barcodes.zip`, 'all-allocation-barcodes.zip');
            } catch (err) {
                alert(err.message || 'Failed to download');
            } finally {
                downloadAllZipBtn.disabled = false;
            }
        });
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await api('/allocations', {
                method: 'POST',
                body: JSON.stringify({
                    inventoryId: document.getElementById('inventoryId').value,
                    departmentId: document.getElementById('departmentId').value,
                    quantity: Number(document.getElementById('allocQuantity').value)
                })
            });
            window.location.reload();
        } catch (err) {
            alert(err.message || 'Failed to allocate');
        }
    });
}

loadDashboard().catch(console.error);
setupInventoryForm().then(loadInventory).catch(console.error);
setupAllocation().catch(console.error);
