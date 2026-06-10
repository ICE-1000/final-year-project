requireAuth();

async function setupGenerator() {
    const button = document.getElementById('newBarcode');
    if (!button) return;
    const input = document.getElementById('barcodeText');
    const image = document.getElementById('barcodeImage');
    async function refreshImage(value) {
        image.src = `${API_BASE}/barcode/image/${encodeURIComponent(value)}`;
    }
    button.addEventListener('click', async () => {
        let value = input.value.trim();
        if (!value) {
            const data = await api('/barcode/new?prefix=INV');
            value = data.barcode;
            input.value = value;
        }
        refreshImage(value);
    });
}

function setupScanner() {
    const container = document.getElementById('scanner-container');
    if (!container || typeof Quagga === 'undefined') return;
    Quagga.init({
        inputStream: { name: 'Live', type: 'LiveStream', target: container },
        decoder: { readers: ['code_128_reader'] }
    }, function(err) {
        if (err) {
            document.getElementById('scanResult').textContent = err.message || 'Camera unavailable';
            return;
        }
        Quagga.start();
    });
    Quagga.onDetected(async function(data) {
        const barcode = data.codeResult.code;
        document.getElementById('scanResult').textContent = barcode;
        Quagga.stop();
        try {
            const item = await api(`/inventory/${encodeURIComponent(barcode)}`);
            document.getElementById('scanItem').textContent = JSON.stringify(item, null, 2);
        } catch (err) {
            document.getElementById('scanItem').textContent = err.message;
        }
    });
}

setupGenerator().catch(console.error);
setupScanner();
