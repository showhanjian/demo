package agent.controller;

import agent.util.ApiResponse;
import agent.util.ChatRequest;
import agent.util.Exceptions;
import agent.util.ExecutionContext;
import agent.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

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
    // 正在处理的 session 集合，防止并发
    private final Set<String> processingSessions = ConcurrentHashMap.newKeySet();

    public ChatController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * 发送消息 - 立即返回，后台异步执行
     */
    @PostMapping("/send")
    public ApiResponse<Void> send(@RequestBody ChatRequest request) {
        logger.info("==================== [ChatController] 收到请求 ====================");
        logger.info("[ChatController] userId={}, sessionId={}, message={}", request.getUserId(), request.getSessionId(), request.getMessage());

        // 参数校验
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            logger.warn("[ChatController] message为空，抛出异常");
            throw new IllegalArgumentException("message不能为空");
        }

        String userId = request.getUserId() == null || request.getUserId().isBlank()
            ? "default_user" : request.getUserId();
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank()
            ? UUID.randomUUID().toString() : request.getSessionId();

        // 检查是否已经在处理中
        if (processingSessions.contains(sessionId)) {
            logger.warn("[ChatController] sessionId={} 正在处理中，拒绝重复请求", sessionId);
            return ApiResponse.fail("请求正在处理中，请稍后");
        }
        processingSessions.add(sessionId);

        // 立即返回，前台异步执行
        logger.info("[ChatController] 立即返回ApiResponse.ok(), 异步执行开始");

        final String finalUserId = userId;
        final String finalSessionId = sessionId;
        final String finalMessage = request.getMessage();

        executor.submit(() -> {
            logger.info("~~~~ [ChatController] 异步线程开始, sessionId={} ~~~~", finalSessionId);
            ExecutionContext.set(finalUserId, finalSessionId);
            try {
                logger.info("[ChatController] 调用 workflowService.execute(), sessionId={}", finalSessionId);
                String result = workflowService.execute(finalUserId, finalSessionId, finalMessage);
                logger.info("[ChatController] workflow.execute 完成, sessionId={}, result长度={}", finalSessionId, result.length());
            } catch (Exception e) {
                logger.error("[ChatController] 执行异常, sessionId={}, error={}", finalSessionId, e.getMessage(), e);
                throw new Exceptions.AgentExecutionException("工作流执行失败: " + e.getMessage(), e);
            } finally {
                ExecutionContext.clear();
                processingSessions.remove(finalSessionId);
                logger.info("~~~~ [ChatController] 异步线程结束, sessionId={} ~~~~", finalSessionId);
            }
        });

        logger.info("[ChatController] 返回 ApiResponse.ok()");
        return ApiResponse.ok();
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }
}
