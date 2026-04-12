# 理解用户提问，根据技能清单进行技能匹配，匹配好了就行，不要再进一步明确意图，提示用户确认匹配结果即可。
## 输出格式约束
```json
{
  "rerult_context": "返回给用户的回复",
  "intent_change": "可执行的任务意图",
  "business_skills": ["技能ID1", "技能ID2", ...],
  "worker": "INTENT_AGENT",
  "status": "NEED_MORE / CONFIRMED"
}
```