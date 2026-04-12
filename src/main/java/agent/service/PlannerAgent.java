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
import io.agentscope.core.plan.PlanNotebook;
import agent.service.SessionService.WorkerState;
import agent.service.SessionService.Status;
import agent.service.SessionService.Worker;
import agent.util.Events;
import agent.util.ModelConfig;
import agent.util.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Agent3: 计划执行智能体
 */
public class PlannerAgent {

    private static final Logger logger = LoggerFactory.getLogger(PlannerAgent.class);

    private final ReActAgent agent;
    private final InMemoryMemory memory;
    private final String sessionRepoPath;
    private final SessionService sessionService;
    private final String systemRepoPath;
    private final String userRepoPath;
    private final MessageProducer messageProducer;
    private PlanNotebook planNotebook;
    private volatile String currentSessionId;

    public PlannerAgent(ModelConfig modelConfig, String skillRepoPath, String systemRepoPath, String userRepoPath, String sessionRepoPath, MessageProducer messageProducer) {
        this.memory = new InMemoryMemory();
        this.sessionRepoPath = sessionRepoPath;
        this.systemRepoPath = systemRepoPath;
        this.userRepoPath = userRepoPath;
        this.sessionService = new SessionService(sessionRepoPath);
        this.messageProducer = messageProducer;

        SkillBox skillBox = new SkillBox(new Toolkit());
        registerSkills(skillBox, skillRepoPath);

        this.planNotebook = PlanNotebook.builder()
            .maxSubtasks(30)
            .build();

        // 注册报表工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new Tools.ReportTool());

        this.agent = ReActAgent.builder()
            .name("PlannerAgent")
            .sysPrompt("") // 临时空值，在execute时动态设置
            .model(modelConfig.createModel())
            .memory(this.memory)
            .toolkit(toolkit)
            .skillBox(skillBox)
            .planNotebook(planNotebook)
            .maxIters(10)
            .build();

        // 注册计划变化钩子，推送计划事件到前端
        registerPlanHook();
    }

    private void registerPlanHook() {
        planNotebook.addChangeHook("plan-event-pusher", (notebook, plan) -> {
            logger.info("[PlannerAgent] 计划钩子触发, plan={}, currentSessionId={}", plan != null, currentSessionId);

            if (plan == null || currentSessionId == null) {
                logger.info("[PlannerAgent] 钩子提前返回: plan=null||currentSessionId=null");
                return;
            }

            logger.info("[PlannerAgent] 钩子触发, sessionId={}, planName={}", currentSessionId, plan.getName());

            // 构建并发送计划事件
            Events.Event event = Events.buildFromPlanNotebook(notebook, plan);
            logger.info("[PlannerAgent] 构建事件: eventType={}, event={}", event != null ? event.eventType : "null", event);

            if (event != null) {
                messageProducer.push(currentSessionId, "processing", event);
            }
        });
    }

    private void registerSkills(SkillBox skillBox, String skillRepoPath) {
        try {
            Path skillDir = Paths.get(skillRepoPath, "plando_report_wj");
            String skillMd = Files.readString(skillDir.resolve("SKILL.md"));

            java.util.Map<String, String> resources = new java.util.HashMap<>();
            Path sampleMdPath = skillDir.resolve("examples/sample.md");
            if (Files.exists(sampleMdPath)) {
                resources.put("examples/sample.md", Files.readString(sampleMdPath));
            }

            skillBox.registerSkill(SkillUtil.createFrom(skillMd, resources));
            logger.info("[PlannerAgent] Skill加载成功: plando_report_wj");
        } catch (Exception e) {
            logger.error("[PlannerAgent] Skill加载失败: plando_report_wj", e);
        }
    }

    /**
     * 执行任务规划
     * @param userId 用户ID（目录名）
     * @param sessionId 会话ID（userId下的子目录名）
     * @param userInput 用户输入
     * @param state WorkerState（由agent内部更新）
     * @return 执行结果
     */
    public String execute(String userId, String sessionId, String userInput, WorkerState state) {
        this.currentSessionId = sessionId;
        logger.info("[PlannerAgent] 开始执行任务规划, userId={}, sessionId={}", userId, sessionId);

        // 创建 Session，路径: sessions_repo/{userId}/{sessionId}/
        Session session = new JsonSession(Paths.get(sessionRepoPath, userId, sessionId));

        // 加载 Memory
        agent.loadIfExists(session, "PlannerAgent_memory_messages");

        // 构建系统提示词内容
        String sysPrompt = SessionService.loadSystemPrompt(systemRepoPath, userRepoPath, userId);

        // 构建Agent提示词
        String agentPrompt = SessionService.loadAgentPrompt(systemRepoPath, "PlannerAgent");

        // 组装完整输入
        String fullInput = sysPrompt + "\n\n" + agentPrompt + "\n\n用户输入: " + userInput;

        Msg userMsg = Msg.builder()
            .role(MsgRole.USER)
            .name(userId)
            .content(TextBlock.builder().text(fullInput).build())
            .build();

        Msg response = agent.call(userMsg).block();
        String result = response != null ? response.getTextContent() : "";

        // 解析 report_content 字段，推送报表内容到前端
        String reportContent = Events.extractField(result, "report_content");
        if (!reportContent.isEmpty()) {
            messageProducer.push(sessionId, "processing", Events.reportContent(reportContent));
        }

        // 解析结果，更新状态
        String planStatus = Events.extractField(result, "status");
        state.worker = Worker.PLANNER;
        state.status = "CONFIRMED".equals(planStatus) ? Status.CONFIRMED : Status.NEED_MORE;

        // 保存 Memory
        agent.saveTo(session, "PlannerAgent_memory_messages");

        // 持久化 WorkerState
        sessionService.saveState(userId, sessionId, state);

        logger.info("[PlannerAgent] 执行完成, result长度={}", result.length());
        return result;
    }

    public ReActAgent getAgent() { return agent; }
    public InMemoryMemory getMemory() { return memory; }
}
