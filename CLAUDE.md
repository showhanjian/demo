# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Multi-agent system built on AgentScope framework (v1.0.9) with a 3-agent Pipeline architecture.

- **Java**: 17
- **Spring Boot**: 3.2.4
- **Server Port**: 8080
- **前端资源**: `src/main/resources/web/` (静态文件在 `web/`)
- **lib_ref/**: AgentScope 库源码参考 (不可编辑)

**项目根目录**: `D:\My_play\agentScope\demo`

## 构建命令

```bash
# 编译
mvn compile

# 运行（Spring Boot）
mvn spring-boot:run
```

## 环境变量

配置于 `application.properties` 或系统环境变量：

| 变量 | 说明 |
|------|------|
| `ATEST_AUTH_TOKEN` | API key for model calls |
| `ATEST_BASE_URL` | API base URL (e.g., `https://api.minimaxi.com/v1`) |
| `ATEST_MODEL` | Model name (e.g., `MiniMax-M2.7`) |

## 3-Agent Pipeline 架构

```
User Input → IntentAgent → PlannerAgent → (async) SummaryAgent
```

| Agent | 职责 | Skill | maxIters | Memory |
|-------|------|-------|----------|--------|
| IntentAgent | 识别用户意图 | intent_analysis | 5 | InMemoryMemory |
| PlannerAgent | 创建并执行任务计划 | plando_report_wj | 10 | InMemoryMemory + PlanNotebook |
| SummaryAgent | 异步总结会话 | summary_knowledge | 1 | InMemoryMemory |

### Worker 状态机

WorkflowService 使用 `WorkerState {worker, status, count}` 管理会话续跑：

| Worker | Status | 含义 |
|--------|--------|------|
| INTENT | NEED_MORE | IntentAgent 执行中，等待确认 |
| INTENT | CONFIRMED | 意图已确认，进入 PlannerAgent |
| PLANNER | NEED_MORE | PlannerAgent 生成计划中，等待确认 |
| PLANNER | CONFIRMED | 计划已确认，执行中 |
| SUMMARY | - | 流程结束 |

**状态流转**:
```
用户输入 → INTENT+NEED_MORE → INTENT+CONFIRMED → PLANNER+NEED_MORE → PLANNER+CONFIRMED → PLANNER(执行) → SUMMARY(异步)
                                                                                                    ↓
用户新输入 → INTENT+NEED_MORE ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←← (重置)
```

### 存储结构

```
src/main/resources/
├── sessions_repo/{userId}/{sessionId}/     # AgentScope JsonSession 存储
├── skill_repo/                            # 技能定义
│   ├── intent_analysis/
│   ├── plando_report_wj/
│   └── summary_knowledge/
├── system_repo/                           # 系统提示词
│   ├── IntentAgent.md, PlannerAgent.md, SummaryAgent.md
│   └── SYSTEM.md
├── web/                                   # 前端资源
│   ├── index.html, js/chat.js, css/chat.css
└── user_repo/{userId}/                    # 用户级记忆
    ├── SOUL.md
    └── MEMORY.md
```

路径配置在 `application.properties` 的 `app.storage.*` 属性。

## 核心组件

### WorkflowService.java (`agent.service.WorkflowService`)
主控层，协调整个执行流程。根据 WorkerState 状态调度对应 Agent。

### SessionService.java (`agent.service.SessionService`)
会话管理服务，基于 AgentScope 的 `JsonSession` 管理 worker_state 持久化。包含 `Worker`、`Status` 枚举和 `WorkerState` 类定义。WorkerState 有三个字段：worker、status、count。

### Agent 实现模式
```java
this.memory = new InMemoryMemory();
SkillBox skillBox = new SkillBox(new Toolkit());
registerSkills(skillBox, skillRepoPath);

// sysPrompt 组装：SYSTEM.md + SOUL.md + MEMORY.md
this.agent = ReActAgent.builder()
    .name("AgentName")
    .sysPrompt(loadSystemPrompt(systemRepoPath, userRepoPath, userId))
    .model(modelConfig.createModel())
    .memory(this.memory)
    .toolkit(new Toolkit())
    .skillBox(skillBox)
    .maxIters(10)
    .build();
```

### MergedSystemFormatter.java
自定义 Formatter，解决 MiniMax API "user name must be consistent (2013)" 错误。合并多条 system 消息为一条，统一 user name 为 "user01"。

### Events.java (`agent.util.Events`)
统一事件数据结构和构建器。包含 `buildFromPlanNotebook` 方法将 PlanNotebook 状态转换为 SessionEvent 事件。

## 前后端通信模式

采用异步轮询模式：
- 前端 POST `/api/chat/send` 发送消息，后端立即返回 `{"status": "ok"}`
- 后台异步执行（线程池），事件写入 MessageStore
- 前端轮询 GET `/api/events/pull?sessionId=` 获取事件

### 核心组件

| 组件 | 说明 |
|------|------|
| MessageProducer | 内存消息生产者，写入 session 对应的 ConcurrentLinkedQueue |
| MessageConsumer | 消费 MessageProducer 的队列，ack 后标记已消费 |
| ExecutionContext | ThreadLocal 上下文，在异步线程间传递 userId/sessionId |

**注意**: PlanNotebook 注册了 `plan-event-pusher` 钩子，会在计划状态变化时自动推送事件到前端。

### SessionEvent 事件类型

| eventType | 说明 | data |
|-----------|------|------|
| intent_result | 意图识别结果 | JSON 字符串 |
| plan_created | 计划方案已生成 | JSON: {planName, result_context} |
| step_started | 步骤开始执行 | {stepIndex, stepName} |
| step_finished | 步骤执行完成 | {stepIndex, stepName} |
| plan_finished | 计划执行完成 | 结果数据 |
| summary_result | 总结生成完成 | 总结文本 |
| report_content | 报表内容 | 报表文本内容 |
| error | 错误信息 | 错误消息 |

## 工具系统

ReportTools 提供报表查询功能（3步流程）：
- `Tool_get_Report()` - 检查 full_report.csv 是否存在
- `Tool_chk_Report()` - 核对明细金额列合计是否等于最后一行合计金额
- `Tool_dis_Report()` - 获取报表内容，返回 `report_content` 字段供 Agent 推送前端

工具注册在 `Tools.java`，通过 `Toolkit` 注入到 Agent。

## Skill 系统

Skills 位于 `src/main/resources/skill_repo/{skill_name}/`：
- `SKILL.md` - 入口文件（name, description, 使用说明）
- `references/` - 参考文档
- `examples/` - 典型案例

**已注册技能**:
- `intent_analysis` → IntentAgent（意图识别规则 + 技能清单）
- `plando_report_wj` → PlannerAgent（报表查询技能 + 工具定义）
- `summary_knowledge` → SummaryAgent（总结生成模板）
