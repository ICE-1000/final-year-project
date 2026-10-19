requireAuth();

const AGENT_HISTORY_KEY = 'agentChatHistory';
let history = [];

function loadHistory() {
    try {
        const raw = sessionStorage.getItem(AGENT_HISTORY_KEY);
        history = raw ? JSON.parse(raw) : [];
    } catch (err) {
        history = [];
    }
}

function saveHistory() {
    try {
        sessionStorage.setItem(AGENT_HISTORY_KEY, JSON.stringify(history));
    } catch (err) {
    }
}

function renderMessage(role, content, toolsUsed) {
    const log = document.getElementById('chatLog');
    if (!log) return;
    const wrap = document.createElement('div');
    wrap.className = `chat-message chat-message-${role}`;
    const bubble = document.createElement('div');
    bubble.className = 'chat-bubble';
    bubble.textContent = content;
    wrap.appendChild(bubble);
    if (toolsUsed && toolsUsed.length) {
        const meta = document.createElement('div');
        meta.className = 'chat-meta';
        meta.textContent = 'Used: ' + toolsUsed.join(', ');
        wrap.appendChild(meta);
    }
    log.appendChild(wrap);
    log.scrollTop = log.scrollHeight;
}

function renderAllHistory() {
    const log = document.getElementById('chatLog');
    if (!log) return;
    log.innerHTML = '';
    if (history.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'chat-empty muted';
        empty.textContent = 'Ask a question about inventory or requests.';
        log.appendChild(empty);
        return;
    }
    history.forEach(turn => renderMessage(turn.role, turn.content));
}

async function checkAgentStatus() {
    const banner = document.getElementById('agentDisabledBanner');
    const form = document.getElementById('chatForm');
    try {
        const status = await api('/agent/status');
        if (!status.configured) {
            if (banner) banner.style.display = 'block';
            if (form) form.querySelectorAll('input, button').forEach(el => el.disabled = true);
        }
    } catch (err) {
    }
}

async function sendMessage(message) {
    const log = document.getElementById('chatLog');
    const sendBtn = document.getElementById('chatSendBtn');
    const input = document.getElementById('chatInput');

    if (log && log.querySelector('.chat-empty')) log.innerHTML = '';
    renderMessage('user', message);
    history.push({ role: 'user', content: message });
    saveHistory();

    if (sendBtn) sendBtn.disabled = true;
    if (input) input.disabled = true;

    const typingEl = document.createElement('div');
    typingEl.className = 'chat-message chat-message-assistant';
    typingEl.innerHTML = '<div class="chat-bubble chat-typing">Thinking…</div>';
    if (log) { log.appendChild(typingEl); log.scrollTop = log.scrollHeight; }

    try {
        const response = await api('/agent/chat', {
            method: 'POST',
            body: JSON.stringify({ message, history: history.slice(0, -1) })
        });
        typingEl.remove();
        renderMessage('assistant', response.reply, response.toolsUsed);
        history.push({ role: 'assistant', content: response.reply });
        saveHistory();
    } catch (err) {
        typingEl.remove();
        renderMessage('assistant', err.message || 'Something went wrong. Please try again.');
    } finally {
        if (sendBtn) sendBtn.disabled = false;
        if (input) { input.disabled = false; input.focus(); }
    }
}

function setupChatForm() {
    const form = document.getElementById('chatForm');
    const input = document.getElementById('chatInput');
    if (!form || !input) return;

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const message = input.value.trim();
        if (!message) return;
        input.value = '';
        sendMessage(message);
    });

    const clearBtn = document.getElementById('chatClearBtn');
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            history = [];
            saveHistory();
            renderAllHistory();
        });
    }
}

function setupVoiceInput() {
    const micBtn = document.getElementById('chatMicBtn');
    const input = document.getElementById('chatInput');
    if (!micBtn || !input) return;

    const SpeechRecognitionImpl = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognitionImpl) {
        micBtn.disabled = true;
        micBtn.title = 'Voice input is not supported in this browser';
        return;
    }

    const recognition = new SpeechRecognitionImpl();
    recognition.continuous = false;
    recognition.interimResults = true;
    recognition.lang = 'en-US';

    let listening = false;

    recognition.addEventListener('start', () => {
        listening = true;
        micBtn.classList.add('mic-active');
        micBtn.innerHTML = '<i class="fa-solid fa-microphone-lines"></i>';
    });

    recognition.addEventListener('result', (event) => {
        let transcript = '';
        for (let i = 0; i < event.results.length; i++) {
            transcript += event.results[i][0].transcript;
        }
        input.value = transcript;
    });

    recognition.addEventListener('error', (event) => {
        if (event.error !== 'no-speech' && event.error !== 'aborted') {
            alert('Voice input error: ' + event.error);
        }
    });

    function stopUi() {
        listening = false;
        micBtn.classList.remove('mic-active');
        micBtn.innerHTML = '<i class="fa-solid fa-microphone"></i>';
    }

    recognition.addEventListener('end', () => {
        stopUi();
        const text = input.value.trim();
        if (text) {
            input.value = '';
            sendMessage(text);
        }
    });

    micBtn.addEventListener('click', () => {
        if (listening) {
            recognition.stop();
        } else {
            input.value = '';
            try {
                recognition.start();
            } catch (err) {
            }
        }
    });
}

loadHistory();
renderAllHistory();
setupChatForm();
setupVoiceInput();
checkAgentStatus().catch(console.error);
