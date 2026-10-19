requireAuth();

let lastScan = { code: null, at: 0 };
const DUPLICATE_SCAN_WINDOW_MS = 1500;

function beep(success) {
    try {
        const AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (!AudioCtx) return;
        const ctx = new AudioCtx();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.frequency.value = success ? 880 : 220;
        osc.type = 'sine';
        gain.gain.setValueAtTime(0.2, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.2);
    } catch (err) {
        // Non-essential - ignore.
    }
}

/* ============================================================
   Barcode Generator (barcode-generator.html)
   ============================================================ */

async function setupGenerator() {
    const button = document.getElementById('newBarcode');
    if (!button) return;
    const input = document.getElementById('barcodeText');
    const image = document.getElementById('barcodeImage');

    function showImage(value) {
        image.src = `${API_BASE}/barcode/image/${encodeURIComponent(value)}`;
    }

    button.addEventListener('click', async () => {
        let value = input.value.trim();
        if (!value) {
            const data = await api('/barcode/new?prefix=INV');
            value = data.barcode;
            input.value = value;
        }
        showImage(value);
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            const value = input.value.trim();
            if (value) showImage(value);
        }
    });
}

/* ============================================================
   Scan Barcode (scan.html)
   Every physical item has exactly one permanent barcode, assigned once at
   registration - scanning it always returns the full picture: which group it
   belongs to (name/category/brand/specification/condition), its own barcode, and
   - if currently allocated - which department and when.
   ============================================================ */

function renderUnitResult(unit) {
    const container = document.getElementById('scanItem');
    if (!container) return;
    const allocationRows = unit.departmentName ? `
        <tr><th>Allocated To</th><td>${escapeHtml(unit.departmentName)}</td></tr>
        <tr><th>Allocated At</th><td>${unit.allocatedAt ? new Date(unit.allocatedAt).toLocaleString() : '-'}</td></tr>
    ` : '';
    container.innerHTML = `
        <div class="table-wrap">
            <table>
                <tbody>
                    <tr><th>Item</th><td>${escapeHtml(unit.inventoryName)}</td></tr>
                    <tr><th>Unit Barcode</th><td>${escapeHtml(unit.unitBarcode)}</td></tr>
                    <tr><th>Unit #</th><td>${unit.unitNumber}</td></tr>
                    <tr><th>Category</th><td>${escapeHtml(unit.categoryName || '-')} (${escapeHtml(unit.categoryCode || '-')})</td></tr>
                    <tr><th>Brand</th><td>${escapeHtml(unit.brand || '-')}</td></tr>
                    <tr><th>Specification</th><td>${escapeHtml(unit.specification || '-')}</td></tr>
                    <tr><th>Condition</th><td>${escapeHtml(unit.condition || '-')}</td></tr>
                    <tr><th>Status</th><td><span class="status">${escapeHtml(unit.status)}</span></td></tr>
                    ${allocationRows}
                    <tr><th>Description</th><td>${escapeHtml(unit.description || '-')}</td></tr>
                    <tr><th></th><td><button type="button" class="button secondary" id="scanDownloadBtn"><i class="fa-solid fa-download"></i> Download Barcode</button></td></tr>
                </tbody>
            </table>
        </div>
    `;
    const downloadBtn = document.getElementById('scanDownloadBtn');
    if (downloadBtn) {
        downloadBtn.addEventListener('click', async () => {
            downloadBtn.disabled = true;
            try {
                await downloadFile(`${API_BASE}/barcode/image/${encodeURIComponent(unit.unitBarcode)}`, `${unit.unitBarcode}.png`);
            } catch (err) {
                alert('Failed to download: ' + (err.message || 'Unknown error'));
            } finally {
                downloadBtn.disabled = false;
            }
        });
    }
}

function renderScanError(message) {
    const container = document.getElementById('scanItem');
    if (!container) return;
    container.innerHTML = `<p class="error">${escapeHtml(message)}</p>`;
}

async function handleScan(rawCode) {
    const code = (rawCode || '').trim();
    if (!code) return;

    const now = Date.now();
    if (code === lastScan.code && (now - lastScan.at) < DUPLICATE_SCAN_WINDOW_MS) return;
    lastScan = { code, at: now };

    const resultEl = document.getElementById('scanResult');
    if (resultEl) resultEl.textContent = code;

    try {
        const result = await api(`/barcode/scan/${encodeURIComponent(code)}`);
        renderUnitResult(result.unit);
        beep(true);
    } catch (err) {
        renderScanError(err.message || 'No inventory unit found for this barcode.');
        beep(false);
    }
}

function setupManualEntry() {
    const input = document.getElementById('manualBarcode');
    const lookupBtn = document.getElementById('lookupBtn');
    const panel = document.getElementById('scanPanel');
    if (!input) return;

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleScan(input.value);
            input.value = '';
        }
    });
    if (lookupBtn) {
        lookupBtn.addEventListener('click', () => {
            handleScan(input.value);
            input.value = '';
        });
    }

    input.focus();
    if (panel) {
        panel.addEventListener('click', (e) => {
            if (e.target === input) return;
            input.focus();
        });
    }
}

function setupCameraScanner() {
    const toggleBtn = document.getElementById('toggleCameraBtn');
    const container = document.getElementById('scanner-container');
    if (!toggleBtn || !container || typeof Quagga === 'undefined') return;

    let running = false;

    function start() {
        container.style.display = 'block';
        Quagga.init({
            inputStream: { name: 'Live', type: 'LiveStream', target: container },
            decoder: { readers: ['code_128_reader'] }
        }, function (err) {
            if (err) {
                renderScanError(err.message || 'Camera unavailable.');
                return;
            }
            Quagga.start();
            running = true;
            toggleBtn.innerHTML = '<i class="fa-solid fa-camera-slash"></i> Stop Camera';
        });
    }

    function stop() {
        if (running) {
            Quagga.stop();
            running = false;
        }
        container.style.display = 'none';
        toggleBtn.innerHTML = '<i class="fa-solid fa-camera"></i> Use Camera';
    }

    toggleBtn.addEventListener('click', () => {
        if (running) stop(); else start();
    });

    Quagga.onDetected((data) => {
        handleScan(data.codeResult.code);
    });
}

setupGenerator().catch(console.error);
setupManualEntry();
setupCameraScanner();
