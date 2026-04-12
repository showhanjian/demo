package agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 消息消费者 - 前端拉取
 */
@Service
public class MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);

    private final MessageProducer producer;

    public MessageConsumer(MessageProducer producer) {
        this.producer = producer;
    }

    /**
     * 拉取一条消息（FIFO 队列，poll 直接移除）
     * @param sessionId 会话ID
     * @return Message 或 null
     */
    public Message pull(String sessionId) {
        ConcurrentLinkedQueue<Message> queue = producer.getSessionQueues().get(sessionId);
        if (queue == null) {
            logger.info("[MessageConsumer] pull, sessionId={}, 队列不存在", sessionId);
            return null;
        }

        Message msg = queue.peek();
        if (msg == null) {
            logger.info("[MessageConsumer] pull, sessionId={}, 队列为空", sessionId);
            return null;
        }

        // 打印消息内容
        String dataPreview = "null";
        if (msg.data != null) {
            if (msg.data instanceof agent.util.Events.Event) {
                agent.util.Events.Event e = (agent.util.Events.Event) msg.data;
                dataPreview = "Event{eventType=" + e.eventType + ", data=" + (e.data != null ? e.data.toString().substring(0, Math.min(100, e.data.toString().length())) : "null") + "}";
            } else {
                dataPreview = msg.data.getClass().getSimpleName() + ": " + msg.data.toString().substring(0, Math.min(100, msg.data.toString().length()));
            }
        }
        logger.info("[MessageConsumer] pull, sessionId={}, seq={}, status={}, data={}", sessionId, msg.sequence, msg.status, dataPreview);

        // 原子性移除
        Message removed = queue.poll();
        if (removed != null) {
            logger.info("[MessageConsumer] pull, sessionId={}, 已从队列移除 seq={}", sessionId, removed.sequence);
        }
        return removed;
    }

    /**
     * 确认消费（队列模式下，poll 已移除消息，ack 变为空操作）
     * @param sessionId 会话ID
     * @param sequence 消息序号
     */
    public void ack(String sessionId, long sequence) {
        // 队列模式下消息已被 poll 移除，ack 无实际意义
        logger.info("[MessageConsumer] ack, sessionId={}, seq={} (队列模式，已通过poll移除)", sessionId, sequence);
    }

    private String getEventType(Object data) {
        if (data == null) return "null";
        if (data instanceof agent.util.Events.Event) {
            return ((agent.util.Events.Event) data).eventType;
        }
        return data.getClass().getSimpleName();
    }
}
