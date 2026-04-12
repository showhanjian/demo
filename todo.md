Todolist：
============================================================================
  未来架构

  ┌─────────────────────────────────────────────────────────┐
  │                     多实例部署                           │
  │                                                       │
  │   ┌──────┐   ┌──────┐   ┌──────┐                    │
  │   │实例1 │   │实例2 │   │实例3 │   ← K8s 管理        │
  │   └──┬───┘   └──┬───┘   └──┬───┘                    │
  │      │          │          │                          │
  │      └──────────┼──────────┘                          │
  │                 │                                     │
  │         ┌──────▼──────┐                            │
  │         │  Redis 集群 │  ← MessageStore 共享        │
  │         └──────┬──────┘                            │
  │                │                                     │
  │         ┌──────▼──────┐                            │
  │         │   NAS 存储   │  ← sessions_repo 共享     │
  │         └─────────────┘                            │
  └─────────────────────────────────────────────────────┘
1、文件系统未来为NAS
2、MessageStore 内存，未来改为redis InMemoryMessageStore
3、K8s 部署多实例 云原生 
4、Gateway 限流 P4



============================================================================
想做一个新流程调度采用编排器（agent）-工作者模式（subagent），
intent做编排器，另外两个做工作者，
目前WorkflowService先保留，
请查阅lib_ref，分析现有工程，思考提出解决方案





plando_report_wj技能，及报表工具改造：
1、plando_report_wj技能流程包括3步
    （1）生成对应报表
    （2）检查对应报表
    （3）展示对应报表
2、相关工具
    （1）生成对应报表Tool_get_Report,负责检查相关目录下full_report是否存在(仅仅检查)
    （2）检查对应报表Tool_chk_Report,负责检查明细金额列加总是否等最后一行合计金额
    （3）展示对应报表Tool_dis_Report,负责获取报表内容交给agent层，agent层负责推送到前端
3、报表只有一个：full_report.csv文件


但前端并没有按顺序全部逐一在对话框中显示，所以

前后端通讯重构设计

消息内存结构:
sessionId，
data，包括：序号，会话状态，消费情况，消息内容

其中：
序号，后端自增生成
会话状态（processing/completed）
消费情况（0已生产/1已消费），


前后端交互设计：
1、后端写消息内存结构，一个方法完成，整个data一起赋值
其中
    agent推送消息，会话状态processing
    WorkflowService里面break前推送的空消息，会话状态completed

2、前端发送用户消息（http短连接）后，等间隔轮询，时间到就按sessionId，开启循环拉取消息并在对话框展示，
while（1）一条一条的循环拉
    {
        拉（http短连接），拆包解析data，将消息内容进行展示
        设状态为=已消费（http短连接），参数sessionId，序号
        if 会话状态=completed 则
           break退出循环
    }

InMemoryMessageStore和MessageStore不要了，新设计2个类
一个后端用（负责推），里面只有一个方法

另一个前端用（负责拉和修改状态） ，里面2个方法
1、拉取一条消息：逻辑是拉取消费情况=0已生产的一条消息，参数：sessionId，结果为：data，
2、修改状态：逻辑是修改消费情况，参数：sessionId，状态值



messageStore.setStatus(sessionId, "completed");
      131 +                    messageStore.addEvent(sessionId, Events.error("流程结束"));


降低大模型调用次数

为什么要ApiResponse

