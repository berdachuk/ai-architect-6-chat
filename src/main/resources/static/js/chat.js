(function () {
    const config = window.AICHAT_CONFIG;
    const escapeHtml = window.AiChat.escapeHtml;
    const apiHeaders = window.AiChat.apiHeaders;

    let currentAssistantMessage = null;
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
            listEl.innerHTML = '<div class="text-muted">No MCP connections configured.</div>';
            return;
        }

        listEl.innerHTML = connections.map(function (connection) {
            const checked = enabledMcpConnections.has(connection.id) ? ' checked' : '';
            const statusClass = connection.status === 'UP' ? 'mcp-status-up' : 'mcp-status-down';
            const toolLabel = connection.toolCount > 0 ? connection.toolCount + ' tools' : 'no tools';
            return `
                <label class="mcp-connection-item">
                    <input type="checkbox" class="form-check-input mt-1 mcp-toggle"
                           data-connection-id="${connection.id}"${checked}>
                    <span class="mcp-connection-meta">
                        <span class="mcp-connection-name">${escapeHtml(connection.name)}</span>
                        <div class="${statusClass}">${connection.status} · ${toolLabel}</div>
                    </span>
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
        panel.scrollTop = panel.scrollHeight;
    }

    function renderMessage(msg) {
        const roleClass = msg.role === 'user' ? 'message-user' : 'message-assistant';
        const roleBadge = msg.role === 'user' ? 'You' : 'AI';
        const html = marked.parse(msg.content || '');
        const sanitized = DOMPurify.sanitize(html);
        return `
            <div class="message ${roleClass}" data-message-id="${msg.id}">
                <div class="message-role">${roleBadge}</div>
                <div class="message-content">${sanitized}</div>
            </div>`;
    }

    function appendUserMessage(content) {
        const panel = document.getElementById('messagePanel');
        const wrapper = document.createElement('div');
        wrapper.className = 'message message-user';
        wrapper.innerHTML = `
            <div class="message-role">You</div>
            <div class="message-content">${escapeHtml(content)}</div>`;
        panel.appendChild(wrapper);
        panel.scrollTop = panel.scrollHeight;
    }

    function createAssistantMessagePlaceholder() {
        const panel = document.getElementById('messagePanel');
        const wrapper = document.createElement('div');
        wrapper.className = 'message message-assistant';
        wrapper.innerHTML = `
            <div class="message-role">AI</div>
            <div class="message-content"></div>`;
        panel.appendChild(wrapper);
        panel.scrollTop = panel.scrollHeight;
        return wrapper;
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
        currentAssistantMessage = createAssistantMessagePlaceholder();
        openAgentPanel();

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

    function appendToken(text) {
        if (!currentAssistantMessage || text == null) {
            return;
        }
        const contentEl = currentAssistantMessage.querySelector('.message-content');
        contentEl.textContent += text;
        const html = marked.parse(contentEl.textContent);
        contentEl.innerHTML = DOMPurify.sanitize(html);
        const panel = document.getElementById('messagePanel');
        panel.scrollTop = panel.scrollHeight;
    }

    function addAgentActivity(activity) {
        const list = document.getElementById('agentActivityList');
        const entry = document.createElement('div');
        entry.className = 'activity-entry';

        if (activity.type === 'tool_call') {
            const status = activity.status || 'running';
            const source = activity.source ? ' [' + activity.source + ']' : '';
            const toolName = activity.toolName || activity.name || 'unknown';
            let label = '🔧 Tool' + source + ': ' + toolName;
            if (status === 'failed') {
                label += ' (failed)';
            } else if (status === 'done') {
                label += ' (done)';
            }
            entry.textContent = label;
            if (activity.message) {
                const detail = document.createElement('div');
                detail.className = 'activity-detail';
                detail.textContent = activity.message;
                entry.appendChild(detail);
            }
        } else if (activity.type === 'reasoning') {
            entry.innerHTML = '💭 ' + escapeHtml(activity.message || activity.text || '');
        } else if (activity.type === 'todo_update') {
            const steps = (activity.steps || []).map(function (step) {
                return step.description + ' [' + (step.status || 'pending') + ']';
            }).join(' → ');
            entry.textContent = '📋 Plan: ' + steps;
        } else if (activity.type === 'llm_call') {
            entry.textContent = '🤖 LLM call';
        } else {
            entry.textContent = activity.type || 'activity';
        }

        list.appendChild(entry);
        showAgentPanel();
    }

    function handleAgentEvent(event) {
        if (event.type === 'agent_start') {
            document.getElementById('agentPanelTitle').textContent =
                'Agent: ' + (event.agentId || event.name || 'orchestrator');
        } else if (event.type === 'agent_done') {
            collapseAgentPanel();
        }
    }

    function updatePipelineStage(payload) {
        const stage = payload.stage;
        if (!stage) {
            return;
        }
        stageStatus.set(stage, payload.status || 'running');
        renderPipelineStages();
        showAgentPanel();
    }

    function renderPipelineStages() {
        const container = document.getElementById('pipelineStages');
        container.innerHTML = '';
        stageStatus.forEach(function (status, stage) {
            const row = document.createElement('div');
            row.className = 'pipeline-stage';
            const icon = status === 'done' ? '✓' : (status === 'failed' ? '✗' : '▶');
            row.innerHTML = `<span class="stage-icon">${icon}</span><span>${escapeHtml(stage)}</span>`;
            container.appendChild(row);
        });
    }

    function openAgentPanel() {
        document.getElementById('agentActivityList').innerHTML = '';
        document.getElementById('pipelineStages').innerHTML = '';
        stageStatus.clear();
        showAgentPanel();
    }

    function showAgentPanel() {
        document.getElementById('agentPanel').classList.remove('d-none');
        document.getElementById('agentActivityList').classList.remove('d-none');
        document.getElementById('pipelineStages').classList.remove('d-none');
        document.getElementById('toggleAgentPanel').textContent = '−';
    }

    function collapseAgentPanel() {
        const activityList = document.getElementById('agentActivityList');
        const count = activityList.children.length + stageStatus.size;
        if (count > 0) {
            document.getElementById('agentPanelTitle').textContent =
                '▸ ' + count + ' step(s) completed — click to expand';
        }
        activityList.classList.add('d-none');
        document.getElementById('pipelineStages').classList.add('d-none');
        document.getElementById('toggleAgentPanel').textContent = '+';
    }

    function finalizeStream() {
        document.getElementById('composer').disabled = false;
        document.getElementById('sendBtn').disabled = false;
        document.getElementById('composer').focus();

        if (currentAssistantMessage) {
            currentAssistantMessage.classList.add('message-complete');
        }
        currentAssistantMessage = null;
        collapseAgentPanel();
        loadChatList();
    }

    document.getElementById('sendBtn').addEventListener('click', sendMessage);
    document.getElementById('composer').addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            sendMessage();
        }
    });

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

    document.getElementById('toggleAgentPanel').addEventListener('click', function () {
        const list = document.getElementById('agentActivityList');
        if (list.classList.contains('d-none')) {
            showAgentPanel();
        } else {
            collapseAgentPanel();
        }
    });

    loadChatList();
    loadHistory();
    loadMcpPanel();
})();
