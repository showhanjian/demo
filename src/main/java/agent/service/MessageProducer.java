package agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 消息生产者 - 后端推送
 */
@Service
public class MessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);

    private final Map<String, ConcurrentLinkedQueue<Message>> sessionQueues = new ConcurrentHashMap<>();
    private final Map<String, Long> sequences = new ConcurrentHashMap<>();

    /**
     * 推送消息
     * @param sessionId 会话ID
     * @param status 会话状态：processing/completed
     * @param data 消息内容
     */
    public void push(String sessionId, String status, Object data) {
        long seq = sequences.computeIfAbsent(sessionId, k -> 0L) + 1;
        sequences.put(sessionId, seq);

        Message msg = new Message();
        msg.sessionId = sessionId;
        msg.sequence = seq;
        msg.status = status;
        msg.consumed = 0;
        msg.data = data;

        sessionQueues.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>()).offer(msg);

        // 详细日志：打印 data 内容
        String dataPreview = "null";
        if (data != null) {
            if (data instanceof agent.util.Events.Event) {
                agent.util.Events.Event e = (agent.util.Events.Event) data;
                dataPreview = "Event{eventType=" + e.eventType + ", data=" + (e.data != null ? e.data.toString().substring(0, Math.min(100, e.data.toString().length())) : "null") + "}";
            } else {
                dataPreview = data.getClass().getSimpleName() + ": " + data.toString().substring(0, Math.min(100, data.toString().length()));
            }
        }
        logger.info("[MessageProducer] push, sessionId={}, seq={}, status={}, consumed={}, data={}", sessionId, seq, status, msg.consumed, dataPreview);
    }

    private String getEventType(Object data) {
        if (data == null) return "null";
        if (data instanceof agent.util.Events.Event) {
            return ((agent.util.Events.Event) data).eventType;
        }
        return data.getClass().getSimpleName();
    }

    /**
     * 获取消息队列（供 MessageConsumer 共享使用）
     */
    public Map<String, ConcurrentLinkedQueue<Message>> getSessionQueues() {
        return sessionQueues;
    }
}
