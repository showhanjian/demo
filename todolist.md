1、主agent非阻塞改造
2、将intentagent改造成监督者，下挂PlannerAgent（plannotebook），SummaryAgent
3、会话开始时就加载：Bootstrap ，工具注册，技能注册
    - Bootstrap包括：SYSTEM.md  SOUL.md   MEMORY.md
    - 工具注册全部
    - 技能注册，元数据
4、SummaryAgent仿照小龙虾
    压缩旧消息 → 写入 MEMORY.md / HISTORY.md/SKILL下的example

    会话消息块
      │
      ▼
  ┌─────────────────────────────────────────────┐
  │  1. 提取关键事件/决策/话题                     │
  │  2. 调用 LLM 执行 save_memory 工具            │
  │  3. 生成摘要写入 HISTORY.md                   │
  │  4. 提炼事实写入 MEMORY.md                    │
  └─────────────────────────────────────────────┘
      │
      ▼
  ┌─────────────────────────────────────────────┐
  │  MEMORY.md 示例:                             │
  │                                             │
  │  ## 用户偏好                                  │
  │  - 喜欢简洁的代码风格                          │
  │  - 使用 Windows 系统                          │
  │                                             │
  │  ## 项目知识                                  │
  │  - API 使用 OAuth2 认证                       │
  │  - 主要用 PostgreSQL 数据库                   │
  │                                             │
  │  ## 经验教训                                  │
  │  - Windows 下用 findstr 替代 grep            │
  └─────────────────────────────────────────────┘

5、权限
    根据数字湖北用户权限表、机构层次表设计，在工具使用中检查



