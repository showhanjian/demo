package agent.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.util.SkillUtil;
import agent.util.ModelConfig;
import agent.service.SessionService.Worker;
import agent.service.SessionService.Status;
import agent.service.SessionService.WorkerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Agent4: 归纳总结智能体
 */
public class SummaryAgent {

    private static final Logger logger = LoggerFactory.getLogger(SummaryAgent.class);

    private final ReActAgent agent;
    private final InMemoryMemory memory;
    private final ExecutorService executor;
    private final String sessionRepoPath;
    private final SessionService sessionService;
    private final String systemRepoPath;
    private final String userRepoPath;
    private MessageProducer messageProducer;

    public SummaryAgent(ModelConfig modelConfig, String skillRepoPath, String systemRepoPath, String userRepoPath, String sessionRepoPath, MessageProducer messageProducer) {
        this.memory = new InMemoryMemory();
        this.sessionRepoPath = sessionRepoPath;
        this.systemRepoPath = systemRepoPath;
        this.userRepoPath = userRepoPath;
        this.sessionService = new SessionService(sessionRepoPath);
        this.messageProducer = messageProducer;
        this.executor = Executors.newFixedThreadPool(2);

        SkillBox skillBox = new SkillBox(new Toolkit());
        registerSkills(skillBox, skillRepoPath);

        this.agent = ReActAgent.builder()
            .name("SummaryAgent")
            .sysPrompt("") // 临时空值，在execute时动态设置
            .model(modelConfig.createModel())
            .memory(this.memory)
            .toolkit(new Toolkit())
            .skillBox(skillBox)
            .maxIters(1)
            .build();
    }

    private void registerSkills(SkillBox skillBox, String skillRepoPath) {
        try {
            Path skillDir = Paths.get(skillRepoPath, "summary_knowledge");
            String skillMd = Files.readString(skillDir.resolve("SKILL.md"));

            Map<String, String> resources = Map.of(
                "references/template.md", Files.readString(skillDir.resolve("references/template.md")),
                "examples/case_format.md", Files.readString(skillDir.resolve("examples/case_format.md"))
            );

            skillBox.registerSkill(SkillUtil.createFrom(skillMd, resources));
        } catch (Exception e) {
            logger.error("Failed to load summary_knowledge skill: {}", e.getMessage());
        }
    }

    /**
     * 异步执行归纳总结
     * @param userId 用户ID（目录名）
     * @param sessionId 会话ID（userId下的子目录名）
     * @param callback 回调函数
     */
    public void summarizeAsync(String userId, String sessionId, Consumer<String> callback) {
        logger.info("Starting async summarization, userId={}, sessionId={}", userId, sessionId);

        executor.submit(() -> {
            try {
                // 创建 Session，路径: sessions_repo/{userId}/{sessionId}/
                Session session = new JsonSession(Paths.get(sessionRepoPath, userId, sessionId));

                // 加载 Memory
                agent.loadIfExists(session, "SummaryAgent_memory_messages");

                // 构建系统提示词内容
                String sysPrompt = SessionService.loadSystemPrompt(systemRepoPath, userRepoPath, userId);

                // 构建Agent提示词
                String agentPrompt = SessionService.loadAgentPrompt(systemRepoPath, "SummaryAgent");

                // 组装完整输入
                String fullInput = sysPrompt + "\n\n" + agentPrompt + "\n\n请对本次会话进行总结归纳";

                Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .name(userId)
                    .content(TextBlock.builder().text(fullInput).build())
                    .build();

                Msg response = agent.call(userMsg).block();
                String result = response != null ? response.getTextContent() : "";
                logger.info("Async summarization completed, result length: {}", result.length());

                // 保存 Memory: sessions_repo/{userId}/{sessionId}/SummaryAgent_memory_messages.jsonl
                agent.saveTo(session, "SummaryAgent_memory_messages");

                // 重置 WorkerState
                WorkerState state = sessionService.loadOrCreateState(userId, sessionId);
                state.worker = Worker.INTENT;
                state.status = Status.NEED_MORE;
                state.count++;
                sessionService.saveState(userId, sessionId, state);

                if (callback != null) {
                    callback.accept(result);
                }
            } catch (Exception e) {
                logger.error("SummaryAgent execution failed: {}", e.getMessage());
            }
        });
    }

    /**
     * 同步执行归纳总结
     * @param userId 用户ID（目录名）
     * @param sessionId 会话ID（userId下的子目录名）
     * @param userInput 用户输入
     * @return 执行结果
     */
    public String execute(String userId, String sessionId, String userInput) {
        logger.info("[SummaryAgent] 开始执行归纳总结, userId={}, sessionId={}", userId, sessionId);

        // 创建 Session，路径: sessions_repo/{userId}/{sessionId}/
        Session session = new JsonSession(Paths.get(sessionRepoPath, userId, sessionId));

        // 加载 Memory
        agent.loadIfExists(session, "SummaryAgent_memory_messages");

        // 构建系统提示词内容
        String sysPrompt = SessionService.loadSystemPrompt(systemRepoPath, userRepoPath, userId);

        // 构建Agent提示词
        String agentPrompt = SessionService.loadAgentPrompt(systemRepoPath, "SummaryAgent");

        // 组装完整输入
        String fullInput = sysPrompt + "\n\n" + agentPrompt + "\n\n" + userInput;

        Msg userMsg = Msg.builder()
            .role(MsgRole.USER)
            .name(userId)
            .content(TextBlock.builder().text(fullInput).build())
            .build();

        Msg response = agent.call(userMsg).block();
        String result = response != null ? response.getTextContent() : "";

        // 保存 Memory: sessions_repo/{userId}/{sessionId}/SummaryAgent_memory_messages.jsonl
        agent.saveTo(session, "SummaryAgent_memory_messages");

        logger.info("[SummaryAgent] 执行完成, result长度={}", result.length());
        return result;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public ReActAgent getAgent() { return agent; }
    public InMemoryMemory getMemory() { return memory; }
}
