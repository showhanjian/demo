package agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理与自定义异常
 */
public class Exceptions {

    private static final Logger logger = LoggerFactory.getLogger(Exceptions.class);

    public static class AgentExecutionException extends RuntimeException {
        public AgentExecutionException(String message) {
            super(message);
        }

        public AgentExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @RestControllerAdvice
    public static class Handler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
            logger.warn("[Exceptions] 参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "data", e.getMessage()));
        }

        @ExceptionHandler(AgentExecutionException.class)
        public ResponseEntity<Map<String, Object>> handleAgentException(AgentExecutionException e) {
            logger.error("[Exceptions] Agent执行异常: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "data", "执行失败: " + e.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
            logger.error("[Exceptions] 未处理异常: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "data", "系统异常，请稍后重试"));
        }
    }
}
