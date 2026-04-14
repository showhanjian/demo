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

    /**
     * 消息结构
     */
    public static class Msg {
        public String status;
        public Object data;
    }

    private final Map<String, ConcurrentLinkedQueue<Msg>> sessionQueues = new ConcurrentHashMap<>();

    /**
     * 推送消息
     * @param sessionId 会话ID
     * @param status 会话状态：processing/completed
     * @param data 消息内容
     */
    public void push(String sessionId, String status, Object data) {
        Msg msg = new Msg();
        msg.status = status;
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
        logger.info("[MessageProducer] push, sessionId={}, status={}, data={}", sessionId, status, dataPreview);
    }

    /**
     * 获取消息队列（供 MessageConsumer 共享使用）
     */
    public Map<String, ConcurrentLinkedQueue<Msg>> getSessionQueues() {
        return sessionQueues;
    }
}
