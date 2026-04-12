package agent.controller;

import agent.service.Message;
import agent.service.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 事件轮询接口
 */
@RestController
@RequestMapping("/api/events")
public class EventsController {

    private static final Logger logger = LoggerFactory.getLogger(EventsController.class);

    @Autowired
    private MessageConsumer messageConsumer;

    /**
     * 拉取一条消息
     * 注意：返回 200，即使没有消息也返回 null body（不是 404）
     */
    @GetMapping("/pull")
    public ResponseEntity<Message> pull(@RequestParam String sessionId) {
        Message msg = messageConsumer.pull(sessionId);
        // 始终返回 200，null body 表示无消息
        return ResponseEntity.ok().body(msg);
    }

    /**
     * 确认消费（修改状态为已消费）
     */
    @PostMapping("/ack")
    public void ack(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        long sequence = ((Number) body.get("sequence")).longValue();
        messageConsumer.ack(sessionId, sequence);
    }
}
