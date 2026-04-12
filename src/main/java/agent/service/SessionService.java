package agent.service;

import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.state.State;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * SessionService - 会话管理服务
 *
 * 数据存储结构：
 * sessions_repo/{userId}/{sessionId}/
 * ├── worker_state.json
 * ├── IntentAgent_memory_messages.jsonl
 * ├── PlannerAgent_memory_messages.jsonl
 * └── SummaryAgent_memory_messages.jsonl
 */
public class SessionService {

    private final String sessionRepoPath;

    public SessionService(String sessionRepoPath) {
        this.sessionRepoPath = sessionRepoPath;
    }

    /**
     * 保存 worker 状态
     * @param userId 用户ID（目录名）
     * @param sessionId 会话ID（userId下的子目录名）
     * @param state WorkerState
     */
    public void saveState(String userId, String sessionId, WorkerState state) {
        Session session = new JsonSession(Paths.get(sessionRepoPath, userId));

        SessionKey sessionKey = SimpleSessionKey.of(sessionId);
        session.save(sessionKey, "worker_state", state);
    }

    /**
     * 加载或新建 worker 状态
     * @param userId 用户ID（目录名）
     * @param sessionId 会话ID（userId下的子目录名）
     * @return WorkerState
     */
    public WorkerState loadOrCreateState(String userId, String sessionId) {
        Session session = new JsonSession(Paths.get(sessionRepoPath, userId));

        SessionKey sessionKey = SimpleSessionKey.of(sessionId);
        return session.get(sessionKey, "worker_state", WorkerState.class)
                .orElse(new WorkerState());
    }

    /**
     * 加载系统提示词
     * @param systemRepoPath 系统提示词根目录
     * @param userRepoPath 用户数据根目录
     * @param userId 用户ID
     * @return 合并后的系统提示词
     */
    public static String loadSystemPrompt(String systemRepoPath, String userRepoPath, String userId) {
        StringBuilder prompt = new StringBuilder();
        try {
            // 1. System.md - 系统行为准则
            prompt.append("# 系统行为准则\n");
            prompt.append(Files.readString(Paths.get(systemRepoPath, "System.md")));
            prompt.append("\n\n");

            // 2. user_entities.md - 用户身份
            prompt.append("# 用户信息\n");
            if (Files.exists(Paths.get(userRepoPath, userId, "user_entities.md"))) {
                prompt.append(Files.readString(Paths.get(userRepoPath, userId, "user_entities.md")));
            } else {
                prompt.append("(未找到用户信息)");
            }
            prompt.append("\n\n");

            // 3. user_behavior.md - 用户习惯
            prompt.append("# 用户习惯\n");
            if (Files.exists(Paths.get(userRepoPath, userId, "user_behavior.md"))) {
                prompt.append(Files.readString(Paths.get(userRepoPath, userId, "user_behavior.md")));
            } else {
                prompt.append("(未找到用户习惯)");
            }
        } catch (Exception e) {
            throw new RuntimeException("系统提示词加载失败", e);
        }
        return prompt.toString();
    }

    /**
     * 加载 Agent 提示词
     * @param systemRepoPath 系统提示词根目录
     * @param agentName Agent 名称（如 "IntentAgent"）
     * @return 包装后的 Agent 提示词
     */
    public static String loadAgentPrompt(String systemRepoPath, String agentName) {
        StringBuilder prompt = new StringBuilder();
        try {
            prompt.append("# ").append(agentName).append(" 规范\n");
            prompt.append(Files.readString(Paths.get(systemRepoPath, agentName + ".md")));
        } catch (Exception e) {
            throw new RuntimeException(agentName + " 提示词加载失败", e);
        }
        return prompt.toString();
    }

    // ========== Status 枚举 ==========

    public enum Status {
        NEED_MORE, CONFIRMED, PLAN_PENDING
    }

    // ========== WorkerState ==========

    public static class WorkerState implements State {
        public Worker worker;      // 当前工作流
        public Status status;     // NEED_MORE 或 CONFIRMED
        public int count;         // 交互次数

        public WorkerState() {
            this.worker = Worker.INTENT;
            this.status = Status.NEED_MORE;
            this.count = 0;
        }
    }

    // ========== Worker 枚举 ==========

    public enum Worker {
        INTENT, PLANNER, SUMMARY
    }
}
