package agent.controller;

import agent.util.Exceptions;
import agent.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天HTTP API控制器
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final WorkflowService workflowService;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Set<String> processingSessions = ConcurrentHashMap.newKeySet();

    public ChatController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        logger.info("[ChatController] 收到请求, message={}", message);

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message不能为空");
        }

        String userId = (String) body.getOrDefault("userId", "default_user");
        String sessionId = (String) body.getOrDefault("sessionId", UUID.randomUUID().toString());

        if (processingSessions.contains(sessionId)) {
            logger.warn("[ChatController] sessionId={} 正在处理中", sessionId);
            return Map.of("status", "error", "data", "请求正在处理中，请稍后");
        }
        processingSessions.add(sessionId);

        final String finalUserId = userId;
        final String finalSessionId = sessionId;
        final String finalMessage = message;

        executor.submit(() -> {
            try {
                workflowService.execute(finalUserId, finalSessionId, finalMessage);
            } catch (Exception e) {
                logger.error("[ChatController] 执行异常, sessionId={}", finalSessionId, e);
                throw new Exceptions.AgentExecutionException("工作流执行失败: " + e.getMessage(), e);
            } finally {
                processingSessions.remove(finalSessionId);
            }
        });

        return Map.of("status", "success", "data", sessionId);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "success", "data", "ok");
    }
}
