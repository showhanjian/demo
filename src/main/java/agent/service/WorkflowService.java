package agent.service;

import agent.util.Events;
import agent.util.ModelConfig;
import agent.util.Tools;
import agent.service.SessionService.Worker;
import agent.service.SessionService.Status;
import agent.service.SessionService.WorkerState;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorkflowService - 工作流服务
 *
 * 职责：
 * - 接收 userId, sessionId, userInput
 * - 调度 Agent 执行
 * - 只管理 worker_state 持久化，不管 Agent Memory（由各 Agent 自己管理）
 *
 * 数据存储结构：
 * sessions_repo/{userId}/{sessionId}/
 * ├── worker_state.json
 * ├── IntentAgent_memory_messages.jsonl
 * ├── LearningAgent_memory_messages.jsonl
 * ├── PlannerAgent_memory_messages.jsonl
 * └── SummaryAgent_memory_messages.jsonl
 */
@org.springframework.stereotype.Service
public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final IntentAgent intentAgent;
    private final PlannerAgent plannerAgent;
    private final SummaryAgent summaryAgent;
    private final SessionService sessionService;
    private final MessageProducer messageProducer;

    public WorkflowService(ModelConfig modelConfig,
                       String skillRepoPath,
                       String systemRepoPath,
                       String userRepoPath,
                       String sessionRepoPath,
                       MessageProducer messageProducer) {
        Toolkit toolkit = new Toolkit();
        Tools.registerTools(toolkit);

        this.sessionService = new SessionService(sessionRepoPath);
        this.messageProducer = messageProducer;

        this.intentAgent = new IntentAgent(modelConfig, skillRepoPath, systemRepoPath, userRepoPath, sessionRepoPath);
        this.plannerAgent = new PlannerAgent(modelConfig, skillRepoPath, systemRepoPath, userRepoPath, sessionRepoPath, messageProducer);
        this.summaryAgent = new SummaryAgent(modelConfig, skillRepoPath, systemRepoPath, userRepoPath, sessionRepoPath, messageProducer);

        logger.info("[WorkflowService] 初始化完成");
    }

    /**
     * 执行工作流
     * @param userId 用户ID（目录名）
     * @param sessionId 会话ID（userId下的子目录名）
     * @param userInput 用户输入
     * @return Agent响应字符串
     */
    public String execute(String userId, String sessionId, String userInput) {
        logger.info("###### [WorkflowService] execute 开始, userId={}, sessionId={}, userInput={}", userId, sessionId, userInput);

        String result = "";
        int loopCount = 0;
        while(true)
        {
            loopCount++;
            if (loopCount > 10) {
                logger.error("[WorkflowService] 循环次数超过10次，可能存在死循环，强制退出");
                break;
            }

            // 1. 加载或新建 worker 状态
            WorkerState state = sessionService.loadOrCreateState(userId, sessionId);
            logger.info("[WorkflowService] 第{}次循环, WorkerState: worker={}, status={}", loopCount, state.worker, state.status);

            // 2. 根据 (worker, status) 组合调度 Agent
            if (state.worker == Worker.INTENT && state.status == Status.NEED_MORE)
            {
                // 阶段1：识别用户意图
                logger.info("[WorkflowService] ===> 阶段1: IntentAgent");
                result = intentAgent.execute(userId, sessionId, userInput, state);

                // 写入意图结果到 MessageStore（前端轮询获取）
                String extractedStatus = Events.extractField(result, "status");
                logger.info("[WorkflowService] IntentAgent 结果: status={}", extractedStatus);
                messageProducer.push(sessionId, "processing", Events.intentResult(result));

                if ("CONFIRMED".equals(extractedStatus)) {
                    // 意图已确认，提取意图上下文，直接进入生成计划阶段
                    userInput = Events.extractField(result, "intent_change");
                    logger.info("[WorkflowService] 意图已确认(CONFIRMED), 进入生成计划阶段, userInput={}", userInput);

                    continue;
                } else {
                    logger.info("[WorkflowService] 意图未确认(NEED_MORE), 流程结束");
                    messageProducer.push(sessionId, "completed", null);
                    break;
                }

            } else if ((state.worker == Worker.INTENT && state.status == Status.CONFIRMED)
                    || (state.worker == Worker.PLANNER && state.status == Status.NEED_MORE))
            {
                // 阶段2：生成计划
                logger.info("[WorkflowService] ===> 阶段2: PlannerAgent(生成计划)");
                result = plannerAgent.execute(userId, sessionId, userInput, state);

                // 写入计划方案到 MessageStore（前端轮询获取）
                String planName = Events.extractField(result, "planName");
                String extractedStatus = Events.extractField(result, "status");
                logger.info("[WorkflowService] PlannerAgent 结果: planName={}, status={}", planName, extractedStatus);
                messageProducer.push(sessionId, "processing", Events.planCreated(planName, result));

                if ("CONFIRMED".equals(extractedStatus)) {
                    // 计划已确认，进入执行阶段
                    userInput = result;
                    logger.info("[WorkflowService] 计划已确认(CONFIRMED), 进入执行阶段");

                    continue;
                } else {
                    logger.info("[WorkflowService] 计划未确认(NEED_MORE), 流程结束");
                    messageProducer.push(sessionId, "completed", null);
                    break;
                }

            } else if (state.worker == Worker.PLANNER && state.status == Status.CONFIRMED) {
                // 阶段3：执行计划
                logger.info("[WorkflowService] ===> 阶段3: PlannerAgent(执行计划)");
                result = plannerAgent.execute(userId, sessionId, userInput, state);
                logger.info("[WorkflowService] PlannerAgent 执行完成, result长度={}", result.length());

                // 计划执行完成，写入事件
                messageProducer.push(sessionId, "completed", Events.planFinished("计划执行完成"));
                break;

            } else if (state.worker == Worker.SUMMARY) {
                logger.info("[WorkflowService] ===> 阶段: SUMMARY");
                result = "流程已完成，请输入新需求";
                messageProducer.push(sessionId, "completed", null);
                break;

            } else {
                logger.warn("[WorkflowService] ===> 未知状态: worker={}, status={}", state.worker, state.status);
                result = "未知状态: worker=" + state.worker + ", status=" + state.status;
                messageProducer.push(sessionId, "completed", null);
                break;
            }
        }

        logger.info("###### [WorkflowService] execute 结束, result长度={}", result.length());
        return result;
    }

    // ========== 生命周期 ==========

    public void shutdown() {
        if (summaryAgent != null) {
            summaryAgent.shutdown();
        }
    }

    // ========== Getter for Agents (for Studio hooks) ==========

    public IntentAgent getIntentAgent() { return intentAgent; }
    public PlannerAgent getPlannerAgent() { return plannerAgent; }
    public SummaryAgent getSummaryAgent() { return summaryAgent; }
}
