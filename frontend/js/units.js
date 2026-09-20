// units.js - shared unit-barcode table rendering, download, and modal helpers.

function unitsTableHtml(units) {
    if (!units || units.length === 0) {
        return '<p class="muted">No units found.</p>';
    }
    return `
        <div class="table-wrap">
            <table>
                <thead><tr><th>#</th><th>Barcode</th><th>Batch</th><th>Status</th><th>Department</th><th></th></tr></thead>
                <tbody>
                    ${units.map(u => `
                        <tr>
                            <td>${u.unitNumber}</td>
                            <td><code>${escapeHtml(u.unitBarcode)}</code></td>
                            <td>${escapeHtml(u.batchCode || '-')}</td>
                            <td><span class="status">${escapeHtml(u.status)}</span></td>
                            <td>${escapeHtml(u.departmentName || '-')}</td>
                            <td><button type="button" class="button secondary" data-download-unit="${escapeHtml(u.unitBarcode)}"><i class="fa-solid fa-download"></i> Download</button></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

function wireUnitDownloadButtons(container) {
    container.querySelectorAll('[data-download-unit]').forEach(btn => {
        btn.addEventListener('click', async () => {
            const code = btn.getAttribute('data-download-unit');
            btn.disabled = true;
            try {
                await downloadFile(`${API_BASE}/barcode/image/${encodeURIComponent(code)}`, `${code}.png`);
            } catch (err) {
                alert('Failed to download barcode: ' + (err.message || 'Unknown error'));
            } finally {
                btn.disabled = false;
            }
        });
    });
}

function ensureUnitsModal() {
    let modal = document.getElementById('unitsModal');
    if (modal) return modal;
    modal = document.createElement('div');
    modal.id = 'unitsModal';
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal-card" style="width:min(92vw,760px);">
            <h2 id="unitsModalTitle">Units</h2>
            <div id="unitsModalBody"></div>
            <div class="actions" style="margin-top:16px;">
                <button type="button" id="unitsModalDownloadPdf" class="button secondary"><i class="fa-solid fa-file-pdf"></i> Download All (PDF Labels)</button>
                <button type="button" id="unitsModalDownloadZip" class="button secondary"><i class="fa-solid fa-file-zipper"></i> Download All (ZIP)</button>
                <button type="button" id="unitsModalClose" class="button secondary">Close</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    modal.addEventListener('click', (e) => { if (e.target === modal) modal.classList.remove('visible'); });
    modal.querySelector('#unitsModalClose').addEventListener('click', () => modal.classList.remove('visible'));
    return modal;
}

function wireDownloadAllButton(button, url, filename) {
    button.style.display = url ? 'inline-flex' : 'none';
    button.onclick = async () => {
        button.disabled = true;
        try {
            await downloadFile(url, filename);
        } catch (err) {
            alert('Failed to download: ' + (err.message || 'Unknown error'));
        } finally {
            button.disabled = false;
        }
    };
}

function batchesTableHtml(batches) {
    if (!batches || batches.length === 0) {
        return '<p class="muted">No registration history found.</p>';
    }
    return `
        <div class="table-wrap">
            <table>
                <thead><tr><th>Batch Code</th><th>Quantity</th><th>Registered By</th><th>Date</th></tr></thead>
                <tbody>
                    ${batches.map(b => `
                        <tr>
                            <td><code>${escapeHtml(b.batchCode)}</code></td>
                            <td>${b.quantityRegistered}</td>
                            <td>${escapeHtml(b.registeredByUsername || '-')}</td>
                            <td>${b.registeredAt ? new Date(b.registeredAt).toLocaleString() : '-'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

// Reuses the same modal shell as openUnitsModal, with the download buttons hidden -
// registration history isn't itself a downloadable barcode set.
function openBatchesModal(title, batches) {
    const modal = ensureUnitsModal();
    modal.querySelector('#unitsModalTitle').textContent = title;
    const body = modal.querySelector('#unitsModalBody');
    body.innerHTML = batchesTableHtml(batches);
    modal.querySelector('#unitsModalDownloadPdf').style.display = 'none';
    modal.querySelector('#unitsModalDownloadZip').style.display = 'none';
    modal.classList.add('visible');
}
// title: shown as the modal heading.
// units: array of InventoryUnitDTO from the backend.
// downloads: optional { pdfUrl, zipUrl } - full URLs (including API_BASE) to the
// label-sheet PDF and/or ZIP endpoints for this set of units. Omit a key (or the
// whole object) to hide that download button.
function openUnitsModal(title, units, downloads) {
    const modal = ensureUnitsModal();
    modal.querySelector('#unitsModalTitle').textContent = title;
    const body = modal.querySelector('#unitsModalBody');
    body.innerHTML = unitsTableHtml(units);
    wireUnitDownloadButtons(body);

    const opts = downloads || {};
    wireDownloadAllButton(modal.querySelector('#unitsModalDownloadPdf'), opts.pdfUrl, 'barcodes.pdf');
    wireDownloadAllButton(modal.querySelector('#unitsModalDownloadZip'), opts.zipUrl, 'barcodes.zip');

    modal.classList.add('visible');
}
