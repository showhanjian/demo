package agent.util;

/**
 * 线程上下文，用于在异步执行时传递 sessionId 和 userId
 */
public class ExecutionContext {

    private static final ThreadLocal<String> sessionId = new ThreadLocal<>();
    private static final ThreadLocal<String> userId = new ThreadLocal<>();

    public static void set(String userId, String sessionId) {
        ExecutionContext.userId.set(userId);
        ExecutionContext.sessionId.set(sessionId);
    }

    public static String getSessionId() {
        return sessionId.get();
    }

    public static String getUserId() {
        return userId.get();
    }

    public static void clear() {
        userId.remove();
        sessionId.remove();
    }
}
