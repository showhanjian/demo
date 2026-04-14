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
     * @return Msg 或 null
     */
    public MessageProducer.Msg pull(String sessionId) {
        ConcurrentLinkedQueue<MessageProducer.Msg> queue = producer.getSessionQueues().get(sessionId);
        if (queue == null) {
            logger.info("[MessageConsumer] pull, sessionId={}, 队列不存在", sessionId);
            return null;
        }

        MessageProducer.Msg msg = queue.peek();
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
        logger.info("[MessageConsumer] pull, sessionId={}, status={}, data={}", sessionId, msg.status, dataPreview);

        // 原子性移除
        return queue.poll();
    }
}
