湖北建行AI融合应用方案
4.关于融合总体方案
4.1.整体架构

整体架构说明：
层级分类	核心模块	核心职能
终端应用层	一体化员工服务平台、柜面员工渠道、速行员工数字通道（均可挂载 AI 助手）	面向不同岗位员工的终端入口，AI 助手作为核心交互载体
应用服务层	服务注册与发布	统一管理所有业务服务的注册、发布、调用，实现服务标准化
外部整合层	外联三方（人行、税务、烟草等）、外部数据（P5 穿山甲、外部数据 P12）	对接外部机构与第三方数据，为内部业务提供外部数据支撑
产品服务层	现有数字湖北业务场景层、功能层、模型层、数据层	核心业务能力封装层，承接终端需求，调用底层模型与数据
技术中台层	短信 / 邮件、EDA 消息订阅、数据集成 / 数仓、总行数据中台	基础技术支撑，负责消息通知、数据存储 / 集成、中台能力复用
分行能体层	特色接入层、智能代理层、特色能力层、特色管理层	数字智能体核心技术底座，提供总行AI 交互、流程编排、知识管理、任务推理等
4.2.系统架构

系统架构特点：
4.2.1.总分协同
总行与分行、前端与后台、运行与管理边界解耦，灵活支持总行技术演进。
4.2.2.上下联动
特色接入：
系统的 “门面”，实现安全入口，用户接入、身份核验与会话管理。
智能代理：
系统的 “大脑”，主要核心特点
1、技术选型合规，使用总行大模型及相关技术框架，独立POD资源访问隔离安全。
2、架构设计灵活，采用多智能体抽象团队分工流程，业务泛化较强。外部系统负责状态和边界，内部模型负责推理，减少核心逻辑频繁调整。
		（1）识别分配者：负责识别、理解、分配，知识技能广而不深。
		（2）计划执行者：负责计划分解，执行、检查，专业领域知识工具。
		（3）分析归纳者：负责对整个过程和结果，分析归纳，经验总结。
3、更多智能可能，采用React循环，并支持技能动态加载扩展。
（1）智能体内部（感知->决策->执行->反馈）提供知识规则和工具说明，感知任务自行决策，调工具执行，根据反馈结果进行再感知反思迭代。
（2）技能根据情况动态加载扩展，目前支持技能、权限、提示词可配置管理，一切皆技能（运维查问题、开发做工具、配置修改，写技能的技能等）
（3）自主更新用户经验习惯和技能成功失败案例，越用越聪明。
4、渐进上下文工程，多层记忆支撑，渐进组织流程，渐进技能披露=>稳定可控。
（1）多层记忆支撑，系统级、用户级、会话级、技能级及内存/文件/DB长短表现。
（2）渐进组织流程，识别分配者->计划执行者||分析归纳者，线性工作边界清晰。
（3）渐进技能披露，元数据->CONTEXT详细说明->参考说明。
特色能力：
系统的 “执行器”，负责提供特色知识技能SKILL、特色工具API，结果可视化支持。
特色管理：
系统的 “运维配置管理”，独立于主流程的，包括：用户权限、用户记忆、提示词、技能工具、审计跟踪、参数配置。
记忆承载：
承载分层记忆的支撑，提供上下文的能力，实践Harness管控
		
5.应用融合概要设计
5.1.记忆承载设计
5.1.1.总体思路原则
上下文窗口有限 → 分层渐进使用记忆，技能渐进披露
信息冗余噪声大 → 进行上下文清洗、降噪去重、摘要压缩（暂不做）
易漂移丢失任务 → 通过上下文强制注入，专注任务目标和安全规范
状态丢失不追溯 → 会话状态持久化管理，会话任务能续跑
重复犯错不记得 → 总结归纳成功失败、案例策略、用户偏好等

5.1.2.承载存储结构
根据上述思路，结合目前行里应用情况简化设计：
长期记忆（文件）/
├── system_repo/   # 系统级
│   ├── SYSTEM.md                   # 最高行为准则（LLM必加）
│   ├── IntentAgent.md              # 智能体目标 规则、输入输出
│   ├── PlannerAgent.md              # 智能体目标 规则、输入输出
│   ├── SummaryAgent.md             # 智能体目标 规则、输入输出
│   └── backup/                     # 版本备份目录
│       └── {filename}_{time}       # 按时间戳归档
│
├── user_repo/     # 用户级（实体与经验）
│   └── {user_id}/                 # 用户ID
│       ├── SOUL.md       # 用户身份岗位、权限信息（LLM+）
│       ├── MEMORY.md      # 习惯经验沉淀（主动更新，LLM+）
│       └── backup/                # 版本备份目录
│            └── {filename}_{time}   # 按文件名称+时间戳归档
│
├── sessions_repo/  # 会话级（活动场景，事件、状态，一致连贯）
│   └── {user_id}/
│        └── {sessionId}/
│             ├── worker_state.json      # 流程状态
│             ├── intent_memory.jsonl   # 意图分类事件记录
│             ├── planer_memory.jsonl   # 计划任务执行事件记录
│             ├── summary_memory.jsonl   # 归纳更新事件记录
│             └── backup/                # 版本备份目录
│                  └── {filename}_{time}  # 按文件名称+时间戳归档
│
└── skill_repo/   # 技能级（程序性流程、工具，按需加载）
    ├── intent_analysis/           # 意图识别分配
    │   ├── SKILL.md              # 技能定义入口
    │   ├── references/           # 参考文档
    │   │   ├── business.md       # 业务术语转换知识
    │   │   └── skill_list.md     # 业务技能领域说明（可扩展业务领域）
    │   └── examples/             # 典型案例（主动更新）
    │       └── sample.md
    │   
    ├── plando_report_wj/       # 网金报表技能（可扩展、可组合）
    │   ├── SKILL.md
    │   ├── references/            # 参考文档
    │   │   └── plan_template.md  # 计划模板
    │   ├── examples/             # 典型案例
    │   │   └── sample.md
    │   └plando_XXX........................................
    │
    └── Summary_knowledge/       # 总结更新技能
        ├── SKILL.md
        ├── references/           # 参考文档
        │   └── template.md       # 总结模板
        └── examples/              # 案例格式
            └── sample.md

短时记忆（内存）
InMemoryMemory Memory接口的内存实现
List<Msg> messages (CopyOnWriteArrayList) 
Msg(消息对象)  id, name, role, content, metadata, timestamp

知识记忆（SQLite索引向量库）（暂不做，视检索效果情况看）
根据情况针对部分信息较多且不易检索的记忆，采用编辑与查询协同模式，将md映射到SQLite索引向量库，便于搜索。
1、Markdown 是本体 — 删除后 SQLite 索引无用
2、编辑Markdown → 自动重建 SQLite 索引
3、SQLite 只是加速层 — 不存储完整知识，只存 chunk 和向量
4、搜索时：先查 SQLite 找相关 chunk → 返回原文片段，文件路径 + 行号关联

上下文层次：
SYSTEM.md # 最高行为准则
SOUL.md、MEMORY.md #用户属性
UserInput 用户请求信息 + session中的之前的result
IntentAgent.md or PlannerAgent.md or SummaryAgent.md #智能体行为目标	规则、输入输出强约束，组织角色边界隔离。
SKILLS元数据 # 概要说明----（每个agent的skill可动态组装）
SKILLContext # 详细说明
SKLLreferences、examples  # 参考知识
Tools 工具

5.1.3.用忆能力封装
类型	操作说明
Msg消息对象	getTextContent() / getChatUsage()
短时记忆 Memory InMemoryMemory	增：memory.addMessage () - 
删：memory.deleteMessage (index) - 按需删除特定消息
查：memory.getMessages () - buildMessages 时调用
清：memory.clear () - 清空注：
会话持久化 JsonSession	存入：agent.saveTo (session, sessionId)
提取：agent.loadIfExists (session, sessionId)
Skill加载注册	技能装载SkillUtil.createFrom()
SkillBox 注册 --> load_skill_through_path () 按需加载
Markdown 文件	读取：ReadFileTool (view_text_file) - 按行范围读取写入：WriteFileTool (write_text_file/insert_text_file)
5.2.特色接入设计
5.2.1.网关协议适配
功能说明：
统一处理来自PC、移动APP、小程序、SDK、API接口等多渠道的接入请求，完成协议转换、负载均衡和流量控制操作，消除渠道间技术壁垒，为用户提供一致的交互体验。
当前方案：
- 基于建行新一代架构的P4、玉衡的双注册模式
- 支持XML与JSON两种调用方式
- 配合玉衡实现精细化流量限流、熔断管控

5.2.2.用户会话管理
功能说明：
实现用户会话生命周期的管理能力，保证多轮对话的连贯性，为工作流程提供会话能力支持，包括：持续维护会话上下文状态，完成消息流的接收、解析、接续和全流程跟踪。
当前方案：
1. 流程层支持
   - 流程结构（身份、状态、输入、输出）的创建、获取、更新
通过持久化，支持流程多次交互和状态流转、上下文的连贯性
worker_state结构
  "worker" : "INTENT/PLANNER/SUMMARYT",  # 当前工作流
  "status" : "NEED_MORE/DONE"  # 当前工作状态
  "input"  : string  # Agent用户请求
  "result"  : string  # Agent执行结果

2. Agent层支持
   - session结构的创建、获取、追加、清理
通过持久化，支持Agent多次交互和状态流转、上下文的连贯性
5.2.3.用户权限控制
功能说明：
实现用户身份工具+场景权限管控，确保用户系统操作均合法合规，防止越权操作和数据泄露风险，同时通过记忆用户偏好，提升系统交互的个性化和便捷性。
当前方案：
在工具内部，根据user查询MySQL权限配置表进行工作校验。
权限配置表结构：
字段	说明
user_id	用户ID
tool_name	工具名称
operate_type	操作场景（工具参数）
control_require	管控要求
执行流程：
1. 工具被调用时，携带user信息
2. 工具内部根据user_id查询权限配置表
3. 判断用户是否有该工具在对应操作场景（参数）的操作权限
4. 无权限返回错误，有权限执行操作

5.3.智能代理设计
5.3.1.工作流程主控
功能说明：
实现相对固定3个agent渐进式调用（境界边界隔离，状态管理连贯），使得组织分工角色和状态和边界管理。协调每个agent内部相对动态的渐进规划-执行-检查-反馈循环。期望增加业务场景泛化能力，减少核心逻辑频繁调整。
当前方案：
对接用户一次会话请求（外部控制获取消息，循环，及超时管理）
1. 初始化 → 准备环境资源
创建 Toolkit并注册所有工具（考虑工具组再看）
创建SessionService会话结构、及各Agent独立资源环境目录等。
加载worker_state结构【流程状态信息】，若无则新建及初始化 

2. 工作流程调用（userId、sessionId、userInput，state）
INTENT-->PLANNER-->SUMMARY-->INTENT...
switch (state.worker)
case INTENT & NEED_MORE: 意图需要交互明确
result = intentAgent.execute
case (INTENT & DONE) or (PLANNER & NEED_MORE) : 意图分类OK或者计划需要修改交互
如果跨流程则传递上流程会话结果
result = plannerAgent.execute
case (PLANNER & DONE) : 计划任务完成，异步归纳总结
读取全部流程会话分析
result = summaryAgent.summarizeAsync

3. 环境资源回收
同申请


5.3.2.意图识别分配
功能说明：
根据用户输入信息，通过IntentAgent 调用intent_analysis技能，得出可明确可执行的业务任务指令+完成任务所需的业务领域技能。包括：根据语义转换知识business.md对用户输入进行清洗、归一化专业处理，根据特色领域能力说明书skill_list.md精准识别完成任务所需的业务领域技能。有效解决自然语言的歧义性问题，将用户的模糊需求，转化为明确任务和完成任务所需的业务领域技能，为后续智能处理奠定基础。
当前方案：
初始化 --> Bootstrap组装 --> LLM推理 --> Skill激活 --> 输出与传递
1. 初始化
   - 创建独立InMemoryMemory，加载或创建SessionMemory（上文连贯）  
   - 创建 SkillBox，注册skill_repo/环境下intent_analysis技能
   - 加载模型modelConfig等
2. Bootstrap组装
   - 读取：system_repo/SYSTEM.md 系统级
   - 读取：user_repo/SOUL.md + MEMORY.md 用户级
   - 构建用户消息：Msg(name=userId, role=USER, content=userInput)
3. LLM推理
   - 读取：IntentAgent.md 智能体目标 规则、输入输出
   - 加载：Skill_Metas（skill_repo/intent_analysis/SKILL.md的name/description）
   - React执行：agent.call(userMsg).block() → 返回 Msg
   - SkillBox激活（渐进披露），注入load_skill_through_path工具
   - LLM按需调用load_skill_("intent_analysis", "references/business.md")
4. 解析并更新状态
	- 解析结果，parseStatus() 更新 WorkerState持久化
	- 保存会话 Memory 到 JsonSession持久化
5、返回清理
   - 结构化输出：明确任务和完成任务所需的业务领域技能列表
   - 资源回收，返回主流程

5.3.3.任务规划执行
功能说明：
根据解决方案（明确任务+技能列表），通过PlannerAgent动态调用相关技能，（如网金报表查询plando_report_wj技能），分析解决方案生成具体任务计划（执行任务、检查任务），返回用户交互优化确认，用户确认后调度任务执行及跟踪状态，完成任务。
当前方案：
1. 初始化
   - 创建独立InMemoryMemory，加载或创建SessionMemory（上文连贯） 
   - 创建 SkillBox，根据业务领域技能列表（skill_list）动态注册技能
While（skill_list）
{   skill_repo/环境加载、SkillBox注册  }
   - 加载模型modelConfig等
2. Bootstrap组装
   - 读取：system_repo/SYSTEM.md 系统级
   - 读取：user_repo/SOUL.md + MEMORY.md 用户级
   - 构建用户消息：Msg(name=userId, role=USER, content=userInput)
3. LLM推理
   - 读取：PlannerAgent.md 智能体目标 规则、输入输出
   - 加载：Skill_Metas（plando_report_wj/SKILL.md的name/description）
   - 创建 PlanNotebook，限制最大任务次数，支持人工介入
   - React执行：agent.call(userMsg).block() → 返回 Msg
   - SkillBox激活（渐进披露），注入load_skill_through_path工具
   - LLM按需调用load_skill_("plando_report_wj", plan_template.md # 计划模板)
4. Plan创建与执行
   - LLM解析→ create_plan()，包括执行任务、检查任务
   - PlanNotebook自动分解为Subtasks
   - 返回用户确认或优化计划
5. Subtask循环执行
   - for each Subtask:
     - LLM选择工具 → toolkit.execute()
     - finish_subtask() → 更新PlanNotebook状态
     - 验证成功返回或失败结束
6. 解析并更新状态
   - agent.saveTo(session, userId) → PlanNotebook持久化
	- 解析结果，parseStatus() 更新 WorkerState持久化
	- 保存会话 Memory 到 JsonSession持久化
7、返回清理
   - 结构化输出：明确任务和完成任务所需的业务领域技能列表
   - 资源回收，返回主流程

例如：
输入：解决方案
1. 获取全行汇总报表 (date=2024-01-01)
2. 检查全行汇总报表
3. 获取机构汇总报表 (date=2024-01-01, orgLevel=2)
4. 检查机构汇总报表
创建计划并跟踪执行
  Plan: "获取全行及机构报表并逐一检查" 
  Subtasks: 
 	[TODO] 获取全行汇总报表 (BRANCH_SUMMARY) 
[TODO] 检查全行汇总报表
[TODO] 获取机构汇总报表 (ORG_SUMMARY, orgLevel=2) 
[TODO] 检查机构汇总报表
输出：
"status": "COMPLETED"
"results": 
	{"task": "获取全行汇总报表", "status": "DONE", "check": "PASS"}
{"task": "获取机构汇总报表", "status": "DONE", "check": "PASS"}

5.3.4.知识总结更新
功能说明：
根据会话流程中的上下文、及成功失败结果（sessions messages），通过SummaryAgent调用Summary_knowledge技能，分析归纳问题和经验规则，追加及更新用户级习惯策略归纳沉淀MEMORY.md和技能级仓库/参考案例resources/examples），便于下次会话更精准使用。
当前方案：
流程主控 --> 触发summarizeAsync(不等待完成) -->返回结果给用户
                              ↓
                      SummaryAgent异步执行
                              ↓
                      读取会话记录 --> 分析 --> 更新长期记忆
1. 初始化
   - 创建独立InMemoryMemory，加载或创建SessionMemory（上文连贯）  
   - 创建 SkillBox，注册skill_repo/环境下Summary_knowledge技能
   - 加载模型modelConfig等
2. Bootstrap组装
   - 读取：system_repo/SYSTEM.md 系统级
   - 读取：user_repo/SOUL.md + MEMORY.md 用户级
   - 构建用户消息：Msg(name=userId, role=USER, content=userInput)
3. LLM推理React执行
   - 读取：SummaryAgent.md 智能体目标 规则、输入输出
   - 加载：Skill_Metas（Summary_knowledge/SKILL.md的name/description）
   - SkillBox激活（渐进披露），注入load_skill_through_path工具
   - LLM按需调用load_skill_("intent_analysis", "template.md# 总结模板")
   - 分析sessions_repo/{user_id}/memory_messages.jsonl会话过程
   - LLM分析成功经验和失败教训，生成更新内容
（此处上下文较大，需看情况考虑摘要）
4. 解析并更新状态
   - 写入：user_repo/{user_id}/MEMORY.md → 用户习惯策略
   - 写入：skill_repo/*/examples/*.md → 技能案例积累
	- 解析结果，parseStatus() 更新 WorkerState持久化
	- 保存会话 Memory 到 JsonSession持久化
5、返回清理
   - 结构化输出：明确任务和完成任务所需的业务领域技能列表
   - 资源回收，返回主流程

5.4.特色能力设计
5.4.1.技能知识建设
功能说明：
分析整理编写项目相关特色领域Skill（业务知识、流程规则、参考资料），为流程任务执行提供支撑，遵循复用现有数据服务能力和应用能力的封装原则。
当前方案：
1、建设承载结构（便于对接skill应用createFrom）
  skills/
  ├── SKILL.md                    # 入口文件（必需）
  ├── references/                 # 参考文档（可选）
  │   ├── api-doc.md
  │   └── best-practices.md
  ├── examples/                   # 示例代码（可选）
  │   └── sample.java
  └── scripts/                    # 脚本文件（可选）
      └── process.py
2、分析整理编写项目相关skil文档，包括：
（1）SKILL.md
  name: data_analysis
  description: 当分析数据、计算统计或生成报表时使用此技能
  # 数据分析技能
  ## 功能概述
  提供数据分析能力...
  ## 使用说明
  1. 首先...
  2. 然后...
  ## 可用资源
（2）references等/api-doc.md: API文档
（3）examples等/sample.java: 示例代码

5.4.2.工具代码建设
功能说明：
分析整理编写项目相关指标API和工具API，为流程任务执行提供支撑，遵循复用现有数据服务能力和应用能力的封装原则。
当前方案：
	1、分析整理封装所需工具
梳理分析业务需求功能点
通过@Tool实现业务逻辑（如ReportTools、UserTools、OrderTools等）
	2、对接Agent使用工具
启动时
一次性注册所有工具到Toolkit（只注册数据，不绑定工具）
运行时
LLM 调用 load_skill_through_path，激活技能 + 激活对应工具组
只有激活的工具，对 LLM 可见
5.4.3.前端交互管理
功能说明：
实现与用户相关交互展示，方便用户使用体验，提供清晰直观、易于理解的可视化信息。
当前方案：
1、通讯体验改造
原短连接影响体验，拟采用后端消息队列推送+前端间隔查询方式。
2、前端文字图表渲染
通过Agent执行数据结果与前端协同，实现根据参数（1表格、2饼图、3柱状图）转化schema，渲染表格、饼图、柱状图等直观的可视化形式。
3、左侧用户历史会话
用户登录后，自动显示最近10个会话，点击可恢复历史会话。
4、计划任务状态追踪
用户会话中，实时显示任务进度，Subtask完成状态可视化。


5.5.特色管理设计
5.5.1.用户权限维护管理
功能说明：
对MySQL数据库中的权限表进行可视化配置，控制用户对工具的访问。
当前方案：
1、创建用户权限
	根据数字湖北员工权限表，自动创建用户权限映射（菜单权限到工具）。
2、用户权限维护
	提供权限表进行可视化创建、编辑、删除等


5.5.2.用户记忆维护管理
功能说明：
对用户记忆文件user_repo、sessions_repo进行可视化配置管理，包括查询、维护、压缩和归档等。
当前方案：
- 长期记忆：
查看、编辑用户身份和偏好MEMORY.md，SOUL.md
修改更新后，自动备份+时间戳
- 会话记忆：
查看会话历史、备份、恢复、清理sessions_repo
修改更新后，自动备份+时间戳


5.5.3.提示词维护管理
功能说明：
对system_repo下的提示词文件进行可视化配置管理。
当前方案：
- 文件列表：展示所有提示词文件
- 编辑器：新建、编辑、预览提示词
- 版本控制：版本历史、对比、回滚


5.5.4.技能配置维护管理
功能说明：
对skill_repo下的技能文件进行可视化配置管理，包括：技能说明、参考知识、案例的上传编辑、维护更新。
当前方案：
- 技能列表：展示所有技能，支持搜索
- SKILL.md编辑器：编辑技能定义
- 资源管理：references、examples文件管理
- 预览：预览技能内容

6.阶段问题和安排
6.1.前置条件
6.1.1.demo验证技术可行性
1、验证总行大模型API基本性能：
解决性能问题，间歇中断，需沟通总行用户，流量分配。
2、验证总行agentscope框架集成：
解决大模型（消息编码），窗口大小待验证。
验证基本流程+skill+tool组装（多system组装）

6.1.2.申请独立Pod环境隔离
系统资源隔离（cpu+内存），磁盘文件隔离，网络访问受限
是重要安全保证。

6.2.实施规划
6.2.1.事项任务分析

6.2.2.时间人员安排


============================================================================
● 工程结构

  src/main/java/agent/
  ├── ChatApplication.java              # Spring Boot 启动类
  │
  ├── controller/                      # 控制层（HTTP）
  │   ├── ChatController.java          # POST /api/chat/send
  │   └── EventsController.java        # GET  /api/events/{sessionId}
  │
  ├── service/                         # 业务逻辑层
  │   ├── WorkflowService.java         # Agent 调度编排
  │   ├── IntentAgent.java            # Agent1: 意图识别
  │   ├── PlannerAgent.java           # Agent2: 计划生成与执行
  │   ├── SummaryAgent.java           # Agent3: 异步总结
  │   ├── SessionService.java         # 会话状态持久化
  │   ├── MessageStore.java           # 消息存储接口
  │   └── InMemoryMessageStore.java   # 内存实现
  │
  └── util/                            # 工具类
      ├── ApiResponse.java             # 统一响应格式
      ├── ChatRequest.java             # 请求 DTO
      ├── Events.java                  # 事件模型 + 工具方法
      ├── Exceptions.java              # 异常定义
      ├── ExecutionContext.java        # 线程上下文
      ├── MergedSystemFormatter.java   # API 兼容格式化
      ├── ModelConfig.java             # 模型配置
      └── Tools.java                   # 工具注册

  src/main/resources/
  ├── application.properties           # 配置（线程池、存储路径）
  ├── web/                            # 前端资源
  │   ├── index.html
  │   ├── js/chat.js
  │   └── css/chat.css
  ├── sessions_repo/                   # 会话存储（WorkerState + Agent Memory）
  ├── skill_repo/                     # Agent 技能定义
  ├── system_repo/                    # 系统提示词
  └── user_repo/                      # 用户记忆

  ---
  调用关系图

  ┌─────────────────────────────────────────────────────────────────────┐
  │                           前端 (Browser)                              │
  │  index.html + chat.js                                                │
  │  · 发送消息 → POST /api/chat/send                                     │
  │  · 轮询事件 → GET  /api/events/{sessionId}                           │
  └───────────────────────────────┬─────────────────────────────────────┘
                                  │
                                  ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │                      ChatController                                  │
  │  POST /api/chat/send                                                │
  │  · 参数校验（userId, sessionId, message）                             │
  │  · 立即返回 ApiResponse.ok()                                          │
  │  · 异步提交到线程池 → WorkflowService.execute()                        │
  └───────────────────────────────┬─────────────────────────────────────┘
                                  │
                                  ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │                     WorkflowService                                  │
  │  execute(userId, sessionId, userInput)                                │
  │  · 加载 WorkerState (SessionService)                                 │
  │  · 根据 (worker, status) 调度对应 Agent                              │
  │  · 写入事件到 MessageStore                                            │
  │                                                                      │
  │  状态机流转:                                                         │
  │  INTENT+NEED_MORE → IntentAgent                                     │
  │  INTENT+CONFIRMED / PLANNER+NEED_MORE → PlannerAgent (生成计划)       │
  │  PLANNER+CONFIRMED → PlannerAgent (执行计划)                          │
  └───────┬─────────────┬─────────────┬─────────────┬────────────────────┘
          │             │             │             │
          ▼             ▼             ▼             ▼
  ┌───────────────┐ ┌───────────────┐ ┌───────────────────────────────┐
  │  IntentAgent  │ │ PlannerAgent  │ │      SummaryAgent             │
  │  · 意图识别    │ │  · 生成计划    │ │  · 异步总结 (回调方式)         │
  │  · Skill:     │ │  · 执行计划    │ │  · Skill: summary_knowledge   │
  │    intent_    │ │  · Skill:     │ │                               │
  │    analysis   │ │    plando_    │ │  execute() 同步                │
  │  · maxIters=5 │ │    report_wj  │ │  summarizeAsync() 异步          │
  │               │ │  · maxIters= │ │                               │
  │  AgentScope:  │ │    10         │ │  AgentScope:                  │
  │  ReActAgent   │ │               │ │  ReActAgent                   │
  │  InMemory     │ │  AgentScope:  │ │  InMemoryMemory               │
  │  Memory       │ │  ReActAgent   │ │                               │
  │               │ │  InMemory     │ │                               │
  │  Session:     │ │  Memory       │ │                               │
  │  JsonSession  │ │  PlanNotebook │ │                               │
  └───────┬───────┘ │  + Hook      │ │                               │
          │         └───────┬───────┘ └───────────────┬───────────────┘
          │                 │                         │
          ▼                 ▼                         ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │                        MessageStore                                  │
  │  (接口: MessageStore, 实现: InMemoryMessageStore)                     │
  │  · addEvent(sessionId, event)     # 写入事件                         │
  │  · pollEvents(sessionId)          # 拉取并清除                       │
  │  · setStatus(sessionId, status)   # 设置状态 (processing/completed)  │
  │  · getStatus(sessionId)           # 获取状态                         │
  └───────┬─────────────────────────────────────────────────────────────┘
          │
          ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │                    EventsController                                 │
  │  GET /api/events/{sessionId}                                         │
  │  · messageStore.pollEvents() → 返回事件列表                          │
  │  · messageStore.getStatus()   → 返回状态                             │
  └─────────────────────────────────────────────────────────────────────┘

  ---
  Agent 调度状态机

  用户输入
      │
      ▼
  ┌────────────────────┐
  │ INTENT + NEED_MORE │ ←────────────┐ 首次输入
  └────────┬───────────┘              │
           │ IntentAgent.execute()   │
           ▼                         │
      意图识别结果                    │
           │                         │
      ┌────┴────┐                    │
      │ CONFIRM?│                    │
      └────┬────┘                    │
      是   │   否                     │
      ▼    │   ▼                      │
  ┌──────────┐  结束                    │
  │ INTENT  │                         │
  │ CONFIRM │                         │
  └────┬───┘                         │
       │ 状态=CONFIRMED + continue    │
       ▼                             │
  ┌────────────────────┐              │
  │ PLANNER + NEED_MORE│             │
  └────────┬───────────┘              │
           │ PlannerAgent.execute()  │
           ▼                         │
      计划生成                        │
           │                         │
      ┌────┴────┐                    │
      │ CONFIRM?│                    │
      └────┬────┘                    │
      是   │   否                     │
      ▼    │   ▼                      │
  ┌──────────┐  结束                   │
  │ PLANNER  │                        │
  │ CONFIRM  │                        │
  └────┬─────┘                        │
       │ 状态=CONFIRMED + continue    │
       ▼                              │
  ┌────────────────────┐              │
  │ PLANNER (执行阶段) │              │
  └────────┬───────────┘              │
           │ PlannerAgent.execute()  │
           │ (PlanNotebook 执行计划) │
           ▼                         │
      计划执行完成 ────────────────────┘
           │
           ▼
  ┌────────────────────┐
  │ SUMMARY (异步执行)  │
  │ SummaryAgent       │
  │ .summarizeAsync()  │
  └────────────────────┘

  ---
  存储结构

  sessions_repo/
  └── {userId}/
      └── {sessionId}/
          ├── worker_state.json          # WorkerState 状态机状态
          ├── IntentAgent_memory_messages.jsonl
          ├── PlannerAgent_memory_messages.jsonl
          └── SummaryAgent_memory_messages.jsonl

  skill_repo/
  ├── intent_analysis/
  │   ├── SKILL.md
  │   └── references/
  ├── plando_report_wj/
  │   ├── SKILL.md
  │   ├── references/
  │   └── examples/
  └── summary_knowledge/
      ├── SKILL.md
      ├── references/
      └── examples/

  system_repo/
  ├── SYSTEM.md              # 系统行为准则
  ├── IntentAgent.md        # IntentAgent 输出格式约束
  ├── PlannerAgent.md       # PlannerAgent 输出格式约束
  └── SummaryAgent.md

  user_repo/
  └── {userId}/
      ├── SOUL.md   # 用户实体知识
      └── MEMORY.md   # 用户行为偏好
============================================================================
  前后端异步轮询通信方案

  架构图

 前端                    后端                       MessageStore
  │                       │                            │
  │  POST /api/chat/send  │                            │
  │──────────────────────>│                            │
  │   {"status": "ok"}    │                            │
  │<──────────────────────│                            │
  │                       │  executor.submit()          │
  │                       │────────┐                    │
  │                       │         │ 异步执行           │
  │                       │<────────┘                    │
  │                       │                            │
  │                       │  IntentAgent.execute() ──> │ 写入最终结果
  │                       │                            │
  │                       │  PlannerAgent.execute()     │
  │                       │    └─> PlanNotebook 钩子 ─>│ 写入计划事件
  │                       │                            │
  │                       │  SummaryAgent ───────────>│ 写入总结结果
  │                       │                            │
  │                       │  MessageStore.status=done  │
  │                       │                            │
  │  GET /api/events/{id} │                            │
  │<──────────────────────│<───────────────────────────│
  │   [events + result]   │                            │
  │       ...              │                            │
  │  GET /api/events/{id} │                            │
  │<──────────────────────│<───────────────────────────│
  │   [completed]          │                            │           │

============================================================================


  