(function() {
    const messagesContainer = document.getElementById('messages');
    const messageInput = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendBtn');
    const clearBtn = document.getElementById('clearBtn');
    const newChatBtn = document.getElementById('newChatBtn');
    const planProgress = document.getElementById('plan-progress');
    const planSteps = document.getElementById('plan-steps');

    let userId = 'web_user';
    let sessionId = localStorage.getItem('sessionId') || '';
    let planMessageId = null;

    // 初始化sessionId
    if (!sessionId) {
        sessionId = String(Date.now());
        localStorage.setItem('sessionId', sessionId);
    }
    document.getElementById('sessionIdDisplay').textContent = '会话: ' + sessionId;

    console.log('[INIT] chat.js 初始化完成, sessionId=' + sessionId);

    // 添加消息
    function addMessage(content, type) {
        console.log('[addMessage] content=' + content.substring(0, 50) + '..., type=' + type);
        const div = document.createElement('div');
        div.className = 'message ' + type;
        div.innerHTML = '<div class="message-content">' + escapeHtml(content) + '</div><div class="message-time">' + new Date().toLocaleTimeString() + '</div>';
        messagesContainer.appendChild(div);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        console.log('[addMessage] 消息已添加到DOM');
    }

    // HTML转义
    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    // 格式化计划消息
    function formatPlanMessage(data) {
        if (typeof data === 'string') return escapeHtml(data);
        const lines = [];
        lines.push('计划: ' + (data.planName || '未命名'));
        if (data.result_context) lines.push('说明: ' + data.result_context);
        return lines.map(escapeHtml).join('\n');
    }

    // 格式化步骤消息
    function formatStepMessage(stepData, status) {
        const icon = status === 'started' ? '开始' : '完成';
        const name = stepData.stepName || '未知步骤';
        const desc = stepData.stepDescription || '';
        let text = '[' + icon + '] ' + name;
        if (desc) {
            text += '\n   ' + desc;
        }
        if (stepData.stepOutcome) {
            text += '\n结果: ' + stepData.stepOutcome;
        }
        return escapeHtml(text);
    }

    // 添加加载状态
    function addLoadingMessage() {
        console.log('[addLoadingMessage] 添加加载状态');
        const div = document.createElement('div');
        div.className = 'message agent';
        div.id = 'loadingMsg';
        div.innerHTML = '<div class="message-content loading">正在思考</div>';
        messagesContainer.appendChild(div);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        return div;
    }

    function removeLoadingMessage() {
        console.log('[removeLoadingMessage] 移除加载状态');
        const loading = document.getElementById('loadingMsg');
        if (loading) loading.remove();
    }

    // 创建计划消息
    function addPlanMessage(event) {
        console.log('[addPlanMessage] event=', event);
        const div = document.createElement('div');
        div.className = 'message agent';
        div.innerHTML = '<div class="message-content" style="white-space:pre-wrap;">' + formatPlanMessage(event) + '</div>';
        messagesContainer.appendChild(div);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        return div;
    }

    // 更新计划步骤
    function updatePlanMessage(msgDiv, stepData, status) {
        console.log('[updatePlanMessage] status=' + status + ', stepData=', stepData);
        const div = document.createElement('div');
        div.className = 'message agent';
        div.innerHTML = '<div class="message-content" style="white-space:pre-wrap;">' + formatStepMessage(stepData, status) + '</div>';
        messagesContainer.appendChild(div);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // 处理事件
    function handlePlanEvent(event) {
        console.log('[handlePlanEvent] event.eventType=' + event?.eventType);
        console.log('[handlePlanEvent] event=' + JSON.stringify(event).substring(0, 200));

        if (!event || !event.eventType) {
            console.log('[handlePlanEvent] 事件无效, eventType缺失');
            return;
        }

        switch (event.eventType) {
            case 'intent_result':
                try {
                    const match = event.data.match(/```json\s*([\s\S]*?)\s*```/);
                    const jsonStr = match ? match[1] : event.data;
                    const obj = JSON.parse(jsonStr);
                    addMessage(obj.result_context, 'agent');
                } catch (e) {
                    addMessage(event.data, 'agent');
                }
                break;
            case 'plan_created':
                try {
                    const raw = typeof event.data === 'string' ? event.data : JSON.stringify(event.data);
                    const match = raw.match(/```json\s*([\s\S]*?)\s*```/);
                    const obj = JSON.parse(match ? match[1] : raw);
                    addMessage(obj.result_context || obj.planName || raw, 'agent');
                } catch (e) {
                    addMessage(typeof event.data === 'string' ? event.data : JSON.stringify(event.data), 'agent');
                }
                break;
            case 'step_started':
                try {
                    const data = typeof event.data === 'string' ? event.data : JSON.stringify(event.data);
                    const obj = typeof data === 'object' ? data : JSON.parse(data);
                    const startInfo = '开始第 ' + (obj.stepIndex + 1) + ' 步: ' + (obj.stepName || '未知步骤') + '\n' + obj.stepDescription;
                    addMessage(startInfo, 'agent');
                } catch (e) {
                    addMessage('▶ ' + event.data, 'agent');
                }
                break;
            /*case 'step_finished':
                try {
                    const data = typeof event.data === 'string' ? event.data : JSON.stringify(event.data);
                    const obj = typeof data === 'object' ? data : JSON.parse(data);
                    const finishInfo = '第 ' + (obj.stepIndex + 1) + ' 步完成: ' + (obj.stepName || '未知步骤') + '\n' + obj.stepDescription;
                    addMessage(finishInfo, 'agent');
                    if (obj.stepDescription) {
                        addMessage(obj.stepDescription, 'agent');
                    }
                    if (obj.stepOutcome) {
                        addMessage( obj.stepOutcome, 'agent');
                    }
                } catch (e) {
                    addMessage('✓ ' + event.data, 'agent');
                }
                break;
                */
            case 'plan_finished':
                console.log('[handlePlanEvent] case: plan_finished');
                addMessage('计划执行完成', 'agent');
                break;
            case 'summary_result':
                console.log('[handlePlanEvent] case: summary_result');
                addMessage('【总结】\n' + event.data, 'agent');
                break;
            case 'report_content': {
                console.log('[handlePlanEvent] case: report_content');
                const div = document.createElement('div');
                div.className = 'message agent';
                const escaped = escapeHtml('【报表】\n' + event.data).replace(/\n/g, '<br>');
                div.innerHTML = '<div class="message-content">' + escaped + '</div>'
                    + '<div class="message-time">' + new Date().toLocaleTimeString() + '</div>';
                messagesContainer.appendChild(div);
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
                break;
            }
            case 'error':
                console.log('[handlePlanEvent] case: error, event.message=' + event.message);
                addMessage('错误: ' + event.message, 'error');
                break;
            default:
                console.log('[handlePlanEvent] 未知eventType: ' + event.eventType);
        }
        console.log('[handlePlanEvent] 处理完成');
    }

    // 轮询
    async function startPolling() {
        console.log('[startPolling] 开始轮询, sessionId=' + sessionId);
        let loopCount = 0;

        while (true) {
            loopCount++;
            if (loopCount > 50) {
                console.log('[startPolling] 超过50次, 退出轮询');
                addMessage('轮询超时，请重试', 'error');
                break;
            }
            console.log('[startPolling] 第' + loopCount + '次循环');

            const pollIntervalInput = document.getElementById('pollIntervalInput');
            const intervalSec = pollIntervalInput ? Math.max(1, parseInt(pollIntervalInput.value) || 1) : 1;
            const intervalMs = intervalSec * 1000;

            try {
                console.log('[startPolling] 发送pull请求...');
                const resp = await fetch('/api/events/pull?sessionId=' + sessionId);
                console.log('[startPolling] pull响应状态: ' + resp.status);

                if (!resp.ok) {
                    console.log('[startPolling] 响应错误, 等待1秒后重试');
                    await sleep(1000);
                    continue;
                }

                const text = await resp.text();
                console.log('[startPolling] pull响应body长度: ' + text.length);
                const msg = text ? JSON.parse(text) : null;
                console.log('[startPolling] msg=' + (msg ? '非null' : 'null'));
                if (msg) {
                    console.log('[startPolling] msg.status=' + msg.status);
                    console.log('[startPolling] msg.data=' + (msg.data ? JSON.stringify(msg.data).substring(0, 100) : 'null'));
                }

                if (msg && msg.data) {
                    console.log('[startPolling] 有数据, 调用handlePlanEvent...');
                    handlePlanEvent(msg.data);
                }

                if (msg && msg.status === 'completed') {
                    console.log('[startPolling] 收到completed, 退出轮询');
                    break;
                }

                console.log('[startPolling] 等待' + intervalMs + 'ms...');
                await sleep(intervalMs);
            } catch (e) {
                console.error('[startPolling] 异常: ' + e.message);
                await sleep(1000);
            }
        }
        console.log('[startPolling] 轮询结束');
        sendBtn.disabled = false;
        messageInput.focus();
    }

    function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

    // 发送消息
    async function sendMessage() {
        const message = messageInput.value.trim();
        if (!message) return;

        console.log('[sendMessage] ====== 开始发送 ======');
        console.log('[sendMessage] message=' + message);
        console.log('[sendMessage] sessionId=' + sessionId);

        addMessage(message, 'user');
        messageInput.value = '';
        addLoadingMessage();
        sendBtn.disabled = true;

        try {
            console.log('[sendMessage] 发送POST /api/chat/send');
            const resp = await fetch('/api/chat/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({userId, sessionId, message})
            });
            console.log('[sendMessage] 响应状态: ' + resp.status);

            const data = await resp.json();
            console.log('[sendMessage] 响应数据: ' + JSON.stringify(data));
            removeLoadingMessage();

            if (data.status === 'success') {
                console.log('[sendMessage] status=success, 开始轮询');
                startPolling();
            } else {
                console.log('[sendMessage] status=' + data.status + ', data=' + data.data);
                addMessage('错误: ' + data.data, 'error');
                sendBtn.disabled = false;
            }
        } catch (e) {
            console.error('[sendMessage] 异常: ' + e.message);
            removeLoadingMessage();
            addMessage('请求失败: ' + e.message, 'error');
            sendBtn.disabled = false;
        }
    }

    // 清空会话
    function clearSession() {
        console.log('[clearSession] 清空会话');
        sessionId = String(Date.now());
        localStorage.setItem('sessionId', sessionId);
        messagesContainer.innerHTML = '';
        planProgress.style.display = 'none';
        planSteps.innerHTML = '';
        planMessageId = null;
        sendBtn.disabled = false;
        addMessage('会话已清空，请开始新对话', 'agent');
    }

    // 开新对话
    function newChat() {
        console.log('[newChat] 开新对话');
        sessionId = String(Date.now());
        localStorage.setItem('sessionId', sessionId);
        document.getElementById('sessionIdDisplay').textContent = '会话: ' + sessionId;
        messagesContainer.innerHTML = '';
        planProgress.style.display = 'none';
        planSteps.innerHTML = '';
        planMessageId = null;
        sendBtn.disabled = false;
        addMessage('你好！我是demo助手，请输入你的问题。', 'agent');
    }

    sendBtn.addEventListener('click', sendMessage);
    messageInput.addEventListener('keypress', e => { if (e.key === 'Enter') sendMessage(); });
    clearBtn.addEventListener('click', clearSession);
    newChatBtn.addEventListener('click', newChat);

    addMessage('你好！我是demo助手，请输入你的问题。', 'agent');
    messageInput.focus();
    console.log('[INIT] 初始化完成');
})();
