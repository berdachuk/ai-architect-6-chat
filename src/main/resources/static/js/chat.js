(function () {
    const config = window.AICHAT_CONFIG;
    const escapeHtml = window.AiChat.escapeHtml;
    const apiHeaders = window.AiChat.apiHeaders;

    let currentAssistantRow = null;
    let currentAgentPanel = null;
    let currentStageStatus = new Map();
    const stageStatus = new Map();
    let enabledMcpConnections = new Set();

    async function loadMcpPanel() {
        const listEl = document.getElementById('mcpConnectionList');
        const statusEl = document.getElementById('mcpPanelStatus');
        if (!listEl) {
            return;
        }

        try {
            const [catalogResp, selectionResp] = await Promise.all([
                fetch('/api/v1/mcp/connections', { headers: apiHeaders(config.userId) }),
                fetch(`/api/v1/chats/${config.chatId}/mcp`, { headers: apiHeaders(config.userId) })
            ]);

            if (!catalogResp.ok || !selectionResp.ok) {
                listEl.textContent = 'MCP unavailable';
                statusEl.textContent = 'off';
                return;
            }

            const catalog = await catalogResp.json();
            const selection = await selectionResp.json();
            enabledMcpConnections = new Set(selection.connectionIds || []);
            renderMcpConnections(catalog);

            const upCount = catalog.filter(function (item) { return item.status === 'UP'; }).length;
            statusEl.textContent = upCount + '/' + catalog.length + ' up';
        } catch (error) {
            listEl.textContent = 'Could not load MCP catalog';
            statusEl.textContent = 'err';
        }
    }

    function renderMcpConnections(connections) {
        const listEl = document.getElementById('mcpConnectionList');
        if (!connections || connections.length === 0) {
            listEl.innerHTML = '<span class="text-muted">none</span>';
            return;
        }

        listEl.innerHTML = connections.map(function (connection) {
            const checked = enabledMcpConnections.has(connection.id) ? ' checked' : '';
            const statusClass = connection.status === 'UP' ? 'mcp-status-up' : 'mcp-status-down';
            return `
                <label class="mcp-connection-item ${statusClass}">
                    <input type="checkbox" class="mcp-toggle"
                           data-connection-id="${connection.id}"${checked}>
                    <span class="mcp-connection-name">${escapeHtml(connection.name)}</span>
                </label>`;
        }).join('');

        listEl.querySelectorAll('.mcp-toggle').forEach(function (input) {
            input.addEventListener('change', function () {
                toggleMcpConnection(input.dataset.connectionId, input.checked);
            });
        });
    }

    async function toggleMcpConnection(connectionId, enabled) {
        if (enabled) {
            enabledMcpConnections.add(connectionId);
        } else {
            enabledMcpConnections.delete(connectionId);
        }
        await fetch(`/api/v1/chats/${config.chatId}/mcp`, {
            method: 'PUT',
            headers: apiHeaders(config.userId),
            body: JSON.stringify({ connectionIds: Array.from(enabledMcpConnections) })
        });
    }

    async function loadChatList() {
        const resp = await fetch('/api/v1/chats', { headers: apiHeaders(config.userId) });
        if (!resp.ok) {
            return;
        }
        renderChatList(await resp.json());
    }

    function renderChatList(chats) {
        const list = document.getElementById('chatList');
        list.innerHTML = chats.map(function (chat) {
            const active = chat.id === config.chatId ? ' active' : '';
            return `
                <div class="chat-item${active}" data-chat-id="${chat.id}">
                    <a href="/chat/${chat.id}" class="chat-item-link">
                        <div class="chat-name">${escapeHtml(chat.name || 'New Chat')}</div>
                        <div class="chat-meta">${chat.messageCount} messages</div>
                    </a>
                    <button type="button" class="btn btn-sm btn-link rename-chat-btn p-0"
                            data-chat-id="${chat.id}" title="Rename">✎</button>
                    <button type="button" class="btn btn-sm text-danger delete-chat-btn p-0"
                            data-chat-id="${chat.id}" title="Delete">×</button>
                </div>`;
        }).join('');

        list.querySelectorAll('.delete-chat-btn').forEach(function (btn) {
            btn.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                deleteChat(btn.dataset.chatId);
            });
        });

        list.querySelectorAll('.rename-chat-btn').forEach(function (btn) {
            btn.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                renameChat(btn.dataset.chatId);
            });
        });
    }

    async function deleteChat(chatId) {
        if (!confirm('Delete this chat?')) {
            return;
        }
        await fetch(`/api/v1/chats/${chatId}`, {
            method: 'DELETE',
            headers: apiHeaders(config.userId)
        });
        if (chatId === config.chatId) {
            window.location.href = '/';
        } else {
            loadChatList();
        }
    }

    async function renameChat(chatId) {
        const name = prompt('Chat name:');
        if (!name || !name.trim()) {
            return;
        }
        await fetch(`/api/v1/chats/${chatId}/name`, {
            method: 'PUT',
            headers: apiHeaders(config.userId),
            body: JSON.stringify({ name: name.trim() })
        });
        if (chatId === config.chatId) {
            document.title = name.trim() + ' — AI Chat';
        }
        loadChatList();
    }

    async function loadHistory() {
        const resp = await fetch(
            `/api/v1/chats/${config.chatId}/history?limit=100`,
            { headers: apiHeaders(config.userId) }
        );
        if (!resp.ok) {
            return;
        }
        renderMessages(await resp.json());
    }

    function renderMessages(messages) {
        const panel = document.getElementById('messagePanel');
        panel.innerHTML = messages.map(renderMessage).join('');
        panel.querySelectorAll('.message-content').forEach(renderAssistantMarkdown);
        panel.scrollTop = panel.scrollHeight;
    }

    function renderMessage(msg) {
        const roleClass = msg.role === 'user' ? 'message-user' : 'message-assistant';
        const roleBadge = msg.role === 'user' ? 'You' : 'AI';
        const rowClass = msg.role === 'user'
            ? 'chat-message-row chat-message-row--user'
            : 'chat-message-row chat-message-row--assistant';
        return `
            <div class="${rowClass}">
                <div class="message ${roleClass}" data-message-id="${msg.id}">
                    <div class="message-role">${roleBadge}</div>
                    <div class="message-content" data-markdown-source="${escapeHtml(msg.content || '')}"></div>
                </div>
            </div>`;
    }

    function appendUserMessage(content) {
        const panel = document.getElementById('messagePanel');
        const row = document.createElement('div');
        row.className = 'chat-message-row chat-message-row--user';
        const wrapper = document.createElement('div');
        wrapper.className = 'message message-user';
        wrapper.innerHTML = `
            <div class="message-role">You</div>
            <div class="message-content">${escapeHtml(content)}</div>`;
        row.appendChild(wrapper);
        panel.appendChild(row);
        panel.scrollTop = panel.scrollHeight;
    }

    function createAgentPanel() {
        const wrap = document.createElement('div');
        wrap.className = 'agent-panel-wrap agent-panel-active';
        wrap.innerHTML =
            '<div class="agent-panel-expanded small border rounded p-2 bg-light">' +
                '<div class="agent-activity-header small fw-semibold text-muted">' +
                    '<span class="agent-activity-spinner"></span>Agent Progress' +
                    '<button class="agent-activity-collapse" type="button" title="Collapse">\u25b2</button>' +
                '</div>' +
                '<div class="agent-pipeline-stages small mt-1"></div>' +
                '<div class="agent-panel-entries small mt-1"></div>' +
            '</div>' +
            '<div class="agent-panel-summary small border rounded p-2 bg-light d-none" role="button" tabindex="0" aria-expanded="false"></div>';

        const header = wrap.querySelector('.agent-activity-header');
        header.addEventListener('click', function (event) {
            if (event.target.closest('.agent-activity-collapse')) {
                return;
            }
            toggleAgentPanel(wrap);
        });
        const collapseBtn = wrap.querySelector('.agent-activity-collapse');
        collapseBtn.addEventListener('click', function (event) {
            event.stopPropagation();
            toggleAgentPanel(wrap);
        });
        const summary = wrap.querySelector('.agent-panel-summary');
        summary.addEventListener('click', function () {
            expandAgentPanel(wrap);
        });

        return wrap;
    }

    function toggleAgentPanel(panelWrap) {
        const expanded = panelWrap.querySelector('.agent-panel-expanded');
        const summary = panelWrap.querySelector('.agent-panel-summary');
        if (expanded && !expanded.classList.contains('d-none')) {
            collapseAgentPanel(panelWrap);
        } else {
            expandAgentPanel(panelWrap);
        }
    }

    function expandAgentPanel(panelWrap) {
        const expanded = panelWrap.querySelector('.agent-panel-expanded');
        const summary = panelWrap.querySelector('.agent-panel-summary');
        if (summary) {
            summary.classList.add('d-none');
        }
        if (expanded) {
            expanded.classList.remove('d-none');
        }
    }

    function collapseAgentPanel(panelWrap) {
        const expanded = panelWrap.querySelector('.agent-panel-expanded');
        const summary = panelWrap.querySelector('.agent-panel-summary');
        const entries = panelWrap._entries || [];
        const elapsedSec = panelWrap._startMs
            ? Math.max(1, Math.round((Date.now() - panelWrap._startMs) / 1000))
            : 0;
        const agents = new Set(entries.map(function (e) { return e.agentId; }));
        const stageCount = panelWrap._stageStatus ? panelWrap._stageStatus.size : 0;
        const totalSteps = entries.length + stageCount;
        const summaryText = '\u25b8 ' + agents.size + ' agent(s) \u00b7 ' + totalSteps
            + ' step(s) \u00b7 ' + elapsedSec + 's \u2014 click to expand';
        summary.textContent = summaryText;
        summary.setAttribute('aria-expanded', 'false');
        summary.classList.remove('d-none');
        if (expanded) {
            expanded.classList.add('d-none');
        }
    }

    function syncPanelSpinner(panelWrap) {
        const entriesEl = panelWrap.querySelector('.agent-panel-entries');
        if (!entriesEl) {
            return;
        }
        const entryNodes = entriesEl.querySelectorAll('.agent-activity-entry');
        entryNodes.forEach(function (node) {
            const old = node.querySelector('.agent-activity-spinner');
            if (old) {
                old.remove();
            }
        });
        if (panelWrap._streamActive && entryNodes.length > 0) {
            const spinner = document.createElement('span');
            spinner.className = 'agent-activity-spinner';
            entryNodes[entryNodes.length - 1].insertBefore(spinner, entryNodes[entryNodes.length - 1].firstChild);
        }
    }

    function addActivityEntryToPanel(panelWrap, kind, message, agentId) {
        const entriesEl = panelWrap.querySelector('.agent-panel-entries');
        const entries = panelWrap._entries || [];
        entries.push({ kind: kind, message: message, agentId: agentId || 'orchestrator', ts: Date.now() });
        panelWrap._entries = entries;

        const entry = document.createElement('div');
        entry.className = 'agent-activity-entry ' + escapeHtml(kind);
        entry.innerHTML =
            '<div class="fw-semibold">' + escapeHtml(agentId || 'orchestrator') + '</div>' +
            '<div class="text-muted">' + escapeHtml(message) + '</div>';
        entriesEl.appendChild(entry);
        syncPanelSpinner(panelWrap);
        entriesEl.scrollTop = entriesEl.scrollHeight;
    }

    function renderPipelineStages(panelWrap) {
        const container = panelWrap.querySelector('.agent-pipeline-stages');
        if (!container) {
            return;
        }
        container.innerHTML = '';
        const stages = panelWrap._stageStatus || new Map();
        stages.forEach(function (status, stage) {
            const row = document.createElement('div');
            row.className = 'pipeline-stage';
            const icon = status === 'done' ? '\u2713' : (status === 'failed' ? '\u2717' : '\u25b6');
            row.innerHTML = '<span class="stage-icon">' + icon + '</span><span>' + escapeHtml(stage) + '</span>';
            container.appendChild(row);
        });
    }

    function createAssistantMessagePlaceholder() {
        const panel = document.getElementById('messagePanel');
        const row = document.createElement('div');
        row.className = 'chat-message-row chat-message-row--assistant';

        const message = document.createElement('div');
        message.className = 'message message-assistant';
        message.innerHTML =
            '<div class="message-role">AI</div>' +
            '<div class="message-content"></div>';
        row.appendChild(message);

        const agentPanel = createAgentPanel();
        row.appendChild(agentPanel);

        panel.appendChild(row);
        panel.scrollTop = panel.scrollHeight;
        currentAssistantRow = row;
        currentAgentPanel = agentPanel;
        agentPanel._entries = [];
        agentPanel._stageStatus = new Map();
        agentPanel._startMs = Date.now();
        agentPanel._streamActive = true;
        currentStageStatus = agentPanel._stageStatus;
        return message;
    }

    async function sendMessage() {
        const composer = document.getElementById('composer');
        const content = composer.value.trim();
        if (!content) {
            return;
        }

        composer.value = '';
        composer.disabled = true;
        document.getElementById('sendBtn').disabled = true;

        appendUserMessage(content);
        createAssistantMessagePlaceholder();

        try {
            const response = await fetch(`/api/v1/chats/${config.chatId}/messages/stream`, {
                method: 'POST',
                headers: apiHeaders(config.userId),
                body: JSON.stringify({ content })
            });

            if (!response.ok || !response.body) {
                throw new Error('Stream failed');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const chunk = await reader.read();
                if (chunk.done) {
                    break;
                }
                buffer += decoder.decode(chunk.value, { stream: true });
                const events = buffer.split('\n\n');
                buffer = events.pop() || '';
                events.forEach(processSseEvent);
            }

            if (buffer.trim()) {
                processSseEvent(buffer);
            }
        } catch (error) {
            appendToken('\n[Error: could not complete stream]');
        } finally {
            finalizeStream();
        }
    }

    function processSseEvent(eventBlock) {
        const lines = eventBlock.split('\n');
        let eventType = 'message';
        let data = '';

        lines.forEach(function (line) {
            if (line.startsWith('event:')) {
                eventType = line.substring(6).trim();
            } else if (line.startsWith('data:')) {
                data += line.substring(5).trim();
            }
        });

        if (!data) {
            return;
        }

        try {
            const payload = JSON.parse(data);
            switch (eventType) {
                case 'token':
                    appendToken(payload.t ?? payload);
                    break;
                case 'activity':
                    addAgentActivity(payload);
                    break;
                case 'agent':
                    handleAgentEvent(payload);
                    break;
                case 'pipeline_stage':
                    updatePipelineStage(payload);
                    break;
                default:
                    break;
            }
        } catch (error) {
            if (eventType === 'token' || eventType === 'message') {
                appendToken(data);
            }
        }
    }

    function renderAssistantMarkdown(contentEl) {
        if (!contentEl) {
            return;
        }
        const source = contentEl.dataset.markdownSource || contentEl.textContent || '';
        const html = marked.parse(source);
        contentEl.innerHTML = DOMPurify.sanitize(html);
        if (window.hljs) {
            contentEl.querySelectorAll('pre code').forEach(function (block) {
                try {
                    window.hljs.highlightElement(block);
                } catch (e) { /* noop */ }
            });
        }
    }

    function appendToken(text) {
        if (!currentAssistantRow || text == null) {
            return;
        }
        const contentEl = currentAssistantRow.querySelector('.message-content');
        contentEl.dataset.markdownSource = (contentEl.dataset.markdownSource || '') + text;
        const html = marked.parse(contentEl.dataset.markdownSource);
        contentEl.innerHTML = DOMPurify.sanitize(html);
        const panel = document.getElementById('messagePanel');
        panel.scrollTop = panel.scrollHeight;
    }

    function addAgentActivity(activity) {
        if (!currentAgentPanel) {
            return;
        }
        const type = activity.type || 'activity';
        if (type === 'tool_call') {
            const status = activity.status || 'running';
            const source = activity.source ? ' [' + activity.source + ']' : '';
            const toolName = activity.toolName || activity.name || 'unknown';
            let label = 'Tool' + source + ': ' + toolName;
            if (status === 'failed') {
                label += ' (failed)';
            } else if (status === 'done') {
                label += ' (done)';
            }
            if (activity.message) {
                label += ' \u2014 ' + activity.message;
            }
            addActivityEntryToPanel(currentAgentPanel, 'tool_call', label, 'orchestrator');
        } else if (type === 'reasoning') {
            addActivityEntryToPanel(currentAgentPanel, 'reasoning',
                activity.message || activity.text || '', 'orchestrator');
        } else if (type === 'todo_update') {
            const steps = (activity.steps || []).map(function (step) {
                return step.description + ' [' + (step.status || 'pending') + ']';
            }).join(' \u2192 ');
            addActivityEntryToPanel(currentAgentPanel, 'todo_update', 'Plan: ' + steps, 'orchestrator');
        } else if (type === 'llm_call') {
            addActivityEntryToPanel(currentAgentPanel, 'llm',
                activity.message || 'LLM call', activity.clientType || 'llm');
        } else {
            addActivityEntryToPanel(currentAgentPanel, type, activity.message || '', 'orchestrator');
        }
    }

    function handleAgentEvent(event) {
        if (!currentAgentPanel) {
            return;
        }
        if (event.type === 'agent_start') {
            addActivityEntryToPanel(currentAgentPanel, 'start',
                'Starting agent: ' + (event.agentId || event.name || 'orchestrator'),
                event.agentId || 'orchestrator');
        } else if (event.type === 'agent_done') {
            addActivityEntryToPanel(currentAgentPanel, 'done',
                'Completed', event.agentId || 'orchestrator');
        }
    }

    function updatePipelineStage(payload) {
        if (!currentAgentPanel) {
            return;
        }
        const stage = payload.stage;
        if (!stage) {
            return;
        }
        currentAgentPanel._stageStatus.set(stage, payload.status || 'running');
        renderPipelineStages(currentAgentPanel);
    }

    function finalizeStream() {
        document.getElementById('composer').disabled = false;
        document.getElementById('sendBtn').disabled = false;
        document.getElementById('composer').focus();

        if (currentAssistantRow) {
            const message = currentAssistantRow.querySelector('.message');
            if (message) {
                message.classList.add('message-complete');
            }
            const contentEl = currentAssistantRow.querySelector('.message-content');
            renderAssistantMarkdown(contentEl);
        }
        if (currentAgentPanel) {
            currentAgentPanel._streamActive = false;
            currentAgentPanel.classList.remove('agent-panel-active');
            syncPanelSpinner(currentAgentPanel);
            collapseAgentPanel(currentAgentPanel);
        }
        currentAssistantRow = null;
        currentAgentPanel = null;
        currentStageStatus = stageStatus;
        loadChatList();
    }

    document.getElementById('sendBtn').addEventListener('click', sendMessage);
    document.getElementById('composer').addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            sendMessage();
        }
    });

    function initSidebar() {
        const sidebar = document.getElementById('sidebar');
        const toggle = document.getElementById('sidebarToggle');
        if (!sidebar || !toggle) {
            return;
        }
        let backdrop = document.querySelector('.sidebar-backdrop');
        if (!backdrop) {
            backdrop = document.createElement('div');
            backdrop.className = 'sidebar-backdrop';
            document.body.appendChild(backdrop);
        }
        const setOpen = function (open) {
            sidebar.classList.toggle('is-open', open);
            backdrop.classList.toggle('is-open', open);
            toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        };
        toggle.addEventListener('click', function () {
            setOpen(!sidebar.classList.contains('is-open'));
        });
        backdrop.addEventListener('click', function () {
            setOpen(false);
        });
        sidebar.addEventListener('click', function (event) {
            if (event.target.closest('a')) {
                setOpen(false);
            }
        });
    }

    document.getElementById('newChatBtn').addEventListener('click', async function () {
        const resp = await fetch('/api/v1/chats', {
            method: 'POST',
            headers: apiHeaders(config.userId),
            body: JSON.stringify({ name: 'New Chat', agentId: 'auto' })
        });
        const chat = await resp.json();
        window.location.href = '/chat/' + chat.id;
    });

    document.getElementById('deleteAllBtn').addEventListener('click', async function () {
        if (!confirm('Delete all chats?')) {
            return;
        }
        const resp = await fetch('/api/v1/chats', { headers: apiHeaders(config.userId) });
        const chats = await resp.json();
        await Promise.all(chats.map(function (chat) {
            return fetch(`/api/v1/chats/${chat.id}`, {
                method: 'DELETE',
                headers: apiHeaders(config.userId)
            });
        }));
        window.location.href = '/';
    });

    initSidebar();
    loadChatList();
    loadHistory();
    loadMcpPanel();
})();
