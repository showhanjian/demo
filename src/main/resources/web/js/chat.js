(function() {
    const messagesContainer = document.getElementById('messages');
    const messageInput = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendBtn');
    const clearBtn = document.getElementById('clearBtn');
    const planProgress = document.getElementById('plan-progress');
    const planSteps = document.getElementById('plan-steps');

    let userId = 'web_user';
    let sessionId = localStorage.getItem('sessionId') || '';
    let planMessageId = null;

    // 初始化sessionId
    if (!sessionId) {
        sessionId = 'session_' + Date.now();
        localStorage.setItem('sessionId', sessionId);
    }
    document.getElementById('sessionIdDisplay').textContent = '会话: ' + sessionId;

    console.log('[INIT] chat.js 初始化完成, sessionId=' + sessionId);

    // 添加消息
    function addMessage(content, type) {
        console.log('[addMessage] content=' + content.substring(0, 50) + '..., type=' + type);
        const div = document.createElement('div');
        div.className = 'message ' + type;
        div.innerHTML = '<div class="message-content">' + content + '</div><div class="message-time">' + new Date().toLocaleTimeString() + '</div>';
        messagesContainer.appendChild(div);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        console.log('[addMessage] 消息已添加到DOM');
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

    // 创建计划消息（简化版，直接显示原始数据）
    function addPlanMessage(event) {
        console.log('[addPlanMessage] event=', event);
        const div = document.createElement('div');
        div.className = 'message agent';
        div.innerHTML = '<div class="message-content" style="max-width:100%;word-wrap:break-word;white-space:pre-wrap;">' + JSON.stringify(event, null, 2) + '</div>';
        messagesContainer.appendChild(div);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        return div;
    }

    // 更新计划步骤（简化版）
    function updatePlanMessage(msgDiv, stepData, status) {
        console.log('[updatePlanMessage] status=' + status + ', stepData=', stepData);
        const div = document.createElement('div');
        div.className = 'message agent';
        div.innerHTML = '<div class="message-content" style="max-width:100%;word-wrap:break-word;white-space:pre-wrap;">[step_' + status + '] ' + JSON.stringify(stepData, null, 2) + '</div>';
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
                console.log('[handlePlanEvent] case: intent_result, event.data=' + (event.data ? event.data.substring(0, 100) : 'null'));
                addMessage('意图: ' + event.data, 'agent');
                break;
            case 'plan_created':
                console.log('[handlePlanEvent] case: plan_created, event.data类型=' + typeof event.data);
                planMessageId = addPlanMessage(event.data);
                break;
            case 'step_started':
                console.log('[handlePlanEvent] case: step_started, event.data=' + JSON.stringify(event.data).substring(0, 100));
                console.log('[handlePlanEvent] step_started, planMessageId=' + planMessageId);
                if (planMessageId) updatePlanMessage(planMessageId, event.data, 'started');
                else console.log('[handlePlanEvent] step_started跳过: planMessageId为null');
                break;
            case 'step_finished':
                console.log('[handlePlanEvent] case: step_finished, planMessageId=' + planMessageId);
                if (planMessageId) updatePlanMessage(planMessageId, event.data, 'finished');
                break;
            case 'plan_finished':
                console.log('[handlePlanEvent] case: plan_finished');
                addMessage('计划执行完成', 'agent');
                break;
            case 'summary_result':
                console.log('[handlePlanEvent] case: summary_result');
                addMessage('总结: ' + event.data, 'agent');
                break;
            case 'report_content':
                console.log('[handlePlanEvent] case: report_content');
                addMessage('报表: ' + event.data, 'agent');
                break;
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
            console.log('[startPolling] 第' + loopCount + '次循环');

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
                    console.log('[startPolling] msg.sequence=' + msg.sequence + ', msg.status=' + msg.status + ', msg.consumed=' + msg.consumed);
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

                console.log('[startPolling] 等待300ms...');
                await sleep(300);
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

            if (data.success) {
                console.log('[sendMessage] data.success=true, 开始轮询');
                startPolling();
            } else if (data.error) {
                console.log('[sendMessage] data.error=' + data.error);
                addMessage('错误: ' + data.error, 'error');
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
        sessionId = 'session_' + Date.now();
        localStorage.setItem('sessionId', sessionId);
        messagesContainer.innerHTML = '';
        planProgress.style.display = 'none';
        planSteps.innerHTML = '';
        planMessageId = null;
        sendBtn.disabled = false;
        addMessage('会话已清空，请开始新对话', 'agent');
    }

    sendBtn.addEventListener('click', sendMessage);
    messageInput.addEventListener('keypress', e => { if (e.key === 'Enter') sendMessage(); });
    clearBtn.addEventListener('click', clearSession);

    addMessage('你好！我是demo助手，请输入你的问题。', 'agent');
    messageInput.focus();
    console.log('[INIT] 初始化完成');
})();
