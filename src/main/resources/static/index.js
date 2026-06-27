/* ─── Config ─────────────────────────────────────────────────────────────── */
const API_BASE = '';

/* ─── State ──────────────────────────────────────────────────────────────── */
const state = {
    file:      null,
    sessionId: null,
    ready:     false,
    loading:   false,
};

/* ─── DOM refs ───────────────────────────────────────────────────────────── */
const uploadZone     = document.getElementById('uploadZone');
const fileInput      = document.getElementById('fileInput');
const browseBtn      = document.getElementById('browseBtn');
const filePreview    = document.getElementById('filePreview');
const fileName       = document.getElementById('fileName');
const fileSize       = document.getElementById('fileSize');
const removeBtn      = document.getElementById('removeBtn');
const processBtn     = document.getElementById('processBtn');
const processBtnText = document.getElementById('processBtnText');
const statusDot      = document.getElementById('statusDot');
const statusText     = document.getElementById('statusText');
const chatMessages   = document.getElementById('chatMessages');
const chatInput      = document.getElementById('chatInput');
const sendBtn        = document.getElementById('sendBtn');
const clearChatBtn   = document.getElementById('clearChatBtn');

/* ─── Helpers ────────────────────────────────────────────────────────────── */
function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function setStatus(type, text) {
    statusDot.className  = 'status-dot '  + type;
    statusText.className = 'status-text ' + type;
    statusText.textContent = text;
}

function setFile(file) {
    state.file = file;
    fileName.textContent = file.name;
    fileSize.textContent = formatBytes(file.size);
    filePreview.classList.add('visible');
    processBtn.disabled = false;
    // Reset any previous session when a new file is picked
    state.sessionId = null;
    state.ready     = false;
    processBtnText.textContent = 'Process Document';
    setStatus('', 'Ready to process');
    chatInput.disabled = true;
    sendBtn.disabled   = true;
}

function clearFile() {
    state.file      = null;
    state.sessionId = null;
    state.ready     = false;
    fileInput.value = '';
    filePreview.classList.remove('visible');
    processBtn.disabled        = true;
    processBtnText.textContent = 'Process Document';
    setStatus('', 'No document loaded');
    chatInput.disabled = true;
    sendBtn.disabled   = true;
}

function setReady() {
    state.ready = true;
    setStatus('ready', 'Document ready · Ask anything');
    chatInput.disabled = false;
    sendBtn.disabled   = false;
    chatInput.focus();
}

function showEmptyChat(message) {
    chatMessages.innerHTML = '';
    const empty = document.createElement('div');
    empty.id        = 'chatEmpty';
    empty.className = 'chat-empty';
    empty.innerHTML = `
        <div class="chat-empty-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
        </div>
        <p>${message}</p>`;
    chatMessages.appendChild(empty);
}

/* ─── Upload Zone ────────────────────────────────────────────────────────── */
uploadZone.addEventListener('click', (e) => {
    if (e.target === browseBtn || e.target.closest('.remove-btn')) return;
    fileInput.click();
});

browseBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    fileInput.click();
});

fileInput.addEventListener('change', () => {
    if (fileInput.files[0]) setFile(fileInput.files[0]);
});

uploadZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadZone.classList.add('drag-over');
});

['dragleave', 'dragend'].forEach(evt =>
    uploadZone.addEventListener(evt, () => uploadZone.classList.remove('drag-over'))
);

uploadZone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.classList.remove('drag-over');
    const file = e.dataTransfer.files[0];
    if (file) setFile(file);
});

removeBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    clearFile();
});

/* ─── Process Button ─────────────────────────────────────────────────────── */
processBtn.addEventListener('click', async () => {
    if (!state.file || state.loading) return;

    state.loading              = true;
    processBtn.disabled        = true;
    processBtnText.textContent = 'Processing…';
    setStatus('loading', 'Extracting & embedding…');

    try {
        const formData = new FormData();
        formData.append('file', state.file);

        const res = await fetch(`${API_BASE}/api/upload`, {
            method: 'POST',
            body: formData,
            // No Content-Type header — browser sets it with the correct boundary for multipart
        });

        // Try to parse JSON regardless of status so we can read the error field
        let data;
        try {
            data = await res.json();
        } catch {
            throw new Error('Server returned an unexpected response.');
        }

        if (!res.ok) {
            // Backend sends { "error": "..." } on failure
            throw new Error(data.error || `Server error (${res.status})`);
        }

        // data = { sessionId, status, chunkCount }
        state.sessionId = data.sessionId;
        processBtnText.textContent = `Processed · ${data.chunkCount} chunks`;
        setReady();
        showEmptyChat('Ask a question about your document');

    } catch (err) {
        const msg = err.message || 'Processing failed';
        setStatus('error', msg.length > 50 ? 'Processing failed · See console' : msg);
        console.error('[Upload error]', err);
        processBtn.disabled        = false;
        processBtnText.textContent = 'Retry Processing';
    } finally {
        state.loading = false;
    }
});

/* ─── Chat ───────────────────────────────────────────────────────────────── */
function removeEmpty() {
    const existing = document.getElementById('chatEmpty');
    if (existing) existing.remove();
}

function appendMessage(role, content) {
    removeEmpty();

    const wrap = document.createElement('div');
    wrap.className = `message ${role}`;

    const label = document.createElement('div');
    label.className   = 'message-label';
    label.textContent = role === 'user' ? 'You' : 'Assistant';

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';

    if (content === '__typing__') {
        wrap.classList.add('thinking');
        bubble.innerHTML = `<div class="typing-dots"><span></span><span></span><span></span></div>`;
    } else {
        // Preserve line breaks from the response
        bubble.innerText = content;
    }

    wrap.appendChild(label);
    wrap.appendChild(bubble);
    chatMessages.appendChild(wrap);
    chatMessages.scrollTop = chatMessages.scrollHeight;

    return wrap;
}

async function sendMessage() {
    const text = chatInput.value.trim();
    if (!text || !state.ready || state.loading || !state.sessionId) return;

    chatInput.value = '';
    autoResize();
    sendBtn.disabled = true;
    state.loading    = true;

    appendMessage('user', text);
    const thinkingEl = appendMessage('assistant', '__typing__');

    try {
        const res = await fetch(`${API_BASE}/api/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: state.sessionId,
                message:   text,
            }),
        });

        const answer = await res.text();

        if (!res.ok) {
            // answer here is the plain-text error string from ResponseEntity.badRequest().body(...)
            throw new Error(answer || `Server error (${res.status})`);
        }

        thinkingEl.remove();
        appendMessage('assistant', answer);

    } catch (err) {
        thinkingEl.remove();
        const errMsg = err.message || 'Something went wrong. Please try again.';
        appendMessage('assistant', errMsg);
        console.error('[Chat error]', err);

        // If session expired, lock chat and tell user to re-upload
        if (errMsg.toLowerCase().includes('session not found') ||
            errMsg.toLowerCase().includes('expired')) {
            state.ready     = false;
            state.sessionId = null;
            chatInput.disabled = true;
            sendBtn.disabled   = true;
            setStatus('error', 'Session expired · Re-upload document');
        }
    } finally {
        state.loading    = false;
        sendBtn.disabled = false;
        chatInput.focus();
    }
}

sendBtn.addEventListener('click', sendMessage);

chatInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

/* ─── Auto-resize textarea ───────────────────────────────────────────────── */
function autoResize() {
    chatInput.style.height = 'auto';
    chatInput.style.height = Math.min(chatInput.scrollHeight, 120) + 'px';
}
chatInput.addEventListener('input', autoResize);

/* ─── Clear Chat ─────────────────────────────────────────────────────────── */
clearChatBtn.addEventListener('click', () => {
    const msg = state.ready
        ? 'Ask a question about your document'
        : 'Process a document to start asking questions';
    showEmptyChat(msg);
});