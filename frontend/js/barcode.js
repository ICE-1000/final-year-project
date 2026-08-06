requireAuth();

let lastScan = { code: null, at: 0 };
const DUPLICATE_SCAN_WINDOW_MS = 1500;

// Short audio cue on scan result - works without any external asset.
// Fails silently if the browser blocks audio (e.g. before any user interaction).
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
            // No text entered - ask the backend for a fresh, format-correct preview code.
            // Note: this is a PREVIEW only. The barcode actually assigned to an inventory
            // item is generated and uniqueness-checked server-side when the item is saved
            // (see InventoryService.create), not here.
            const data = await api('/barcode/new?prefix=INV');
            value = data.barcode;
            input.value = value;
        }
        showImage(value);
    });

    // Typing/pasting an existing code and pressing Enter previews it directly.
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
   Handles two input paths that both funnel into the same
   handleScan() function:
     1. Hardware USB/Bluetooth scanners - these act as a keyboard,
        "typing" the code into whatever input is focused and then
        sending Enter. The #manualBarcode input handles this for free.
     2. Camera scanning via Quagga2, for staff without a scanner.
   ============================================================ */

function renderInventoryResult(item) {
    const container = document.getElementById('scanItem');
    if (!container) return;
    container.innerHTML = `
        <div class="table-wrap">
            <table>
                <tbody>
                    <tr><th>Type</th><td>Inventory item</td></tr>
                    <tr><th>Name</th><td>${escapeHtml(item.inventoryName)}</td></tr>
                    <tr><th>Barcode</th><td>${escapeHtml(item.barcode)}</td></tr>
                    <tr><th>Category</th><td>${escapeHtml(item.categoryName || '-')} (${escapeHtml(item.categoryCode || '-')})</td></tr>
                    <tr><th>Quantity</th><td>${item.quantity}</td></tr>
                    <tr><th>Available</th><td>${item.availableQuantity}</td></tr>
                    <tr><th>Allocated</th><td>${item.allocatedQuantity}</td></tr>
                    <tr><th>Serial Number</th><td>${escapeHtml(item.serialNumber || '-')}</td></tr>
                    <tr><th>Condition</th><td>${escapeHtml(item.condition || '-')}</td></tr>
                    <tr><th>Status</th><td><span class="status">${escapeHtml(item.status)}</span></td></tr>
                    <tr><th>Description</th><td>${escapeHtml(item.description || '-')}</td></tr>
                </tbody>
            </table>
        </div>
    `;
}

function renderAllocationResult(allocation) {
    const container = document.getElementById('scanItem');
    if (!container) return;
    container.innerHTML = `
        <div class="table-wrap">
            <table>
                <tbody>
                    <tr><th>Type</th><td>Allocation</td></tr>
                    <tr><th>Item</th><td>${escapeHtml(allocation.inventoryName)}</td></tr>
                    <tr><th>Department</th><td>${escapeHtml(allocation.departmentName)}</td></tr>
                    <tr><th>Quantity Allocated</th><td>${allocation.quantity}</td></tr>
                    <tr><th>Status</th><td><span class="status">${escapeHtml(allocation.status)}</span></td></tr>
                    <tr><th>Allocated At</th><td>${allocation.allocatedAt ? new Date(allocation.allocatedAt).toLocaleString() : '-'}</td></tr>
                    <tr><th>Allocation Barcode</th><td>${escapeHtml(allocation.allocationBarcode || '-')}</td></tr>
                </tbody>
            </table>
        </div>
    `;
}

function renderScanError(message) {
    const container = document.getElementById('scanItem');
    if (!container) return;
    container.innerHTML = `<p class="error">${escapeHtml(message)}</p>`;
}

async function handleScan(rawCode) {
    const code = (rawCode || '').trim();
    if (!code) return;

    // Ignore an identical code scanned again within the debounce window - guards
    // against a scanner or the camera firing multiple reads of the same label.
    const now = Date.now();
    if (code === lastScan.code && (now - lastScan.at) < DUPLICATE_SCAN_WINDOW_MS) return;
    lastScan = { code, at: now };

    const resultEl = document.getElementById('scanResult');
    if (resultEl) resultEl.textContent = code;

    try {
        const result = await api(`/barcode/scan/${encodeURIComponent(code)}`);
        if (result.type === 'ALLOCATION') {
            renderAllocationResult(result.allocation);
        } else {
            renderInventoryResult(result.inventory);
        }
        beep(true);
    } catch (err) {
        renderScanError(err.message || 'No inventory item or allocation found for this barcode.');
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

    // Keep this input focused by default so a hardware scanner works the instant
    // this page loads, with no click required. Scoped to clicks inside the scan
    // panel only, so it doesn't hijack focus from sidebar navigation elsewhere
    // on the page.
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

    // Camera access is only requested when the user explicitly asks for it -
    // never automatically on page load.
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
