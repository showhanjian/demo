---
name: intent_analysis
description: 理解用户提问，根据技能清单进行技能匹配，匹配好了就行，不要再进一步明确意图，提示用户确认匹配结果即可。
---
# Intent Analysis Skill

## 业务规则
1. 理解用户提问，根据技能清单进行技能匹配，匹配好了就行，不要再进一步明确意图，提示用户确认匹配结果即可。
2. 如果用户提问与技能清单无关，则："status" ="NEED_MORE"
3. 如果用户输入确认等相关语义，则："status" ="CONFIRMED"


## 技能清单

### 1. plando_report_wj - 报表查询
**触发条件**: 需要查询数据报表时

### 2. plando_cuiniu - 讲笑话
**触发条件**: 需要讲笑话时使用

