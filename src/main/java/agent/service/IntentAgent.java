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
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.util.SkillUtil;
import agent.util.Events;
import agent.util.ModelConfig;
import agent.service.SessionService.WorkerState;
import agent.service.SessionService.Status;
import agent.service.SessionService.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Agent1: 意图识别智能体
 */
public class IntentAgent {

    private static final Logger logger = LoggerFactory.getLogger(IntentAgent.class);

    private final ReActAgent agent;
    private final InMemoryMemory memory;
    private final String sessionRepoPath;
    private final SessionService sessionService;
    private final String systemRepoPath;
    private final String userRepoPath;

    public IntentAgent(ModelConfig modelConfig, String skillRepoPath, String systemRepoPath, String userRepoPath, String sessionRepoPath) {
        this.memory = new InMemoryMemory();
        this.sessionRepoPath = sessionRepoPath;
        this.systemRepoPath = systemRepoPath;
        this.userRepoPath = userRepoPath;
        this.sessionService = new SessionService(sessionRepoPath);

        SkillBox skillBox = new SkillBox(new Toolkit());
        registerSkills(skillBox, skillRepoPath);

        this.agent = ReActAgent.builder()
            .name("IntentAgent")
            .sysPrompt("") // 临时空值，在execute时动态设置
            .model(modelConfig.createModel())
            .memory(this.memory)
            .toolkit(new Toolkit())
            .skillBox(skillBox)
            .maxIters(5)
            .build();
    }

    private void registerSkills(SkillBox skillBox, String skillRepoPath) {
        try {
            Path skillDir = Paths.get(skillRepoPath, "intent_analysis");
            Path skillMdPath = skillDir.resolve("SKILL.md");
            Path businessPath = skillDir.resolve("references").resolve("business.md");
            Path skillListPath = skillDir.resolve("references").resolve("skill_list.md");

            String skillMd = Files.readString(skillMdPath);

            Map<String, String> resources = Map.of(
                "references/business.md", Files.exists(businessPath) ? Files.readString(businessPath) : "",
                "references/skill_list.md", Files.exists(skillListPath) ? Files.readString(skillListPath) : ""
            );

            AgentSkill skill = SkillUtil.createFrom(skillMd, resources);
            skillBox.registerSkill(skill);
            logger.info("[IntentAgent] Skill加载成功: intent_analysis");

        } catch (Exception e) {
            logger.error("[IntentAgent] Skill加载失败: intent_analysis", e);
        }
    }

    /**
     * 执行意图识别
     * @param userId 用户ID（目录名）
     * @param sessionId 会话ID（userId下的子目录名）
     * @param userInput 用户输入
     * @param state WorkerState（由agent内部更新）
     * @return 执行结果
     */
    public String execute(String userId, String sessionId, String userInput, WorkerState state) {
        logger.info("[IntentAgent] 开始执行意图识别, userId={}, sessionId={}", userId, sessionId);

        // 创建 Session，路径: sessions_repo/{userId}/{sessionId}/
        Session session = new JsonSession(Paths.get(sessionRepoPath, userId, sessionId));

        // 加载 Memory
        agent.loadIfExists(session, "IntentAgent_memory_messages");

        // 构建系统提示词内容
        String sysPrompt = SessionService.loadSystemPrompt(systemRepoPath, userRepoPath, userId);

        // 构建Agent提示词
        String agentPrompt = SessionService.loadAgentPrompt(systemRepoPath, "IntentAgent");

        // 组装完整输入
        String fullInput = sysPrompt + "\n\n" + agentPrompt + "\n\n用户输入: " + userInput;

        Msg userMsg = Msg.builder()
            .role(MsgRole.USER)
            .name(userId)
            .content(TextBlock.builder().text(fullInput).build())
            .build();

        Msg response = agent.call(userMsg).block();
        String result = response != null ? response.getTextContent() : "";

        // 解析结果，更新状态
        String intentStatus = Events.extractField(result, "status");
        state.worker = Worker.INTENT;
        state.status = "CONFIRMED".equals(intentStatus) ? Status.CONFIRMED : Status.NEED_MORE;

        // 保存 Memory
        agent.saveTo(session, "IntentAgent_memory_messages");

        // 持久化 WorkerState
        sessionService.saveState(userId, sessionId, state);

        logger.info("[IntentAgent] 执行完成, result长度={}", result.length());
        return result;
    }

    public ReActAgent getAgent() { return this.agent; }
    public InMemoryMemory getMemory() { return this.memory; }
}
