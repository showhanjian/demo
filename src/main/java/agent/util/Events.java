package agent.util;

import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import io.agentscope.core.plan.model.SubTask;
import io.agentscope.core.plan.model.SubTaskState;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一事件数据结构和构建器
 */
public class Events {

    // ========== JSON 工具（从 JsonUtil 合并） ==========

    /**
     * 从JSON字符串中提取指定字段值
     * @param json JSON字符串
     * @param field 字段名
     * @return 字段值，如果不存在返回空字符串
     */
    public static String extractField(String json, String field) {
        if (json == null || json.isEmpty()) return "";

        int fieldStart = json.indexOf("\"" + field + "\"");
        if (fieldStart == -1) return "";

        int colonPos = json.indexOf(":", fieldStart);
        if (colonPos == -1) return "";

        int valueStart = json.indexOf("\"", colonPos);
        if (valueStart == -1) return "";

        int valueEnd = json.indexOf("\"", valueStart + 1);
        return valueEnd > valueStart ? json.substring(valueStart + 1, valueEnd) : "";
    }

    // ========== 事件数据结构 ==========

    public static class Event implements Serializable {
        public String eventType;
        public String sessionId;
        public Object data;
        public String message;
        public long timestamp;

        public Event() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class StepInfo implements Serializable {
        public String stepName;
        public int stepIndex;
        public String status;
        public Object result;

        public StepInfo(String stepName, int stepIndex, String status) {
            this.stepName = stepName;
            this.stepIndex = stepIndex;
            this.status = status;
        }

        public StepInfo(String stepName, int stepIndex, String status, Object result) {
            this.stepName = stepName;
            this.stepIndex = stepIndex;
            this.status = status;
            this.result = result;
        }
    }

    // ========== 事件工厂方法 ==========

    public static Event intentResult(String intent) {
        Event event = new Event();
        event.eventType = "intent_result";
        event.data = intent;
        event.message = "意图识别完成: " + intent;
        return event;
    }

    public static Event planCreated(String planName, Object planData) {
        Event event = new Event();
        event.eventType = "plan_created";
        event.data = planData;
        event.message = "计划创建: " + planName;
        return event;
    }

    public static Event stepStarted(String stepName, int stepIndex) {
        Event event = new Event();
        event.eventType = "step_started";
        event.data = new StepInfo(stepName, stepIndex, "进行中");
        event.message = "开始执行: " + stepName;
        return event;
    }

    public static Event stepFinished(String stepName, int stepIndex, Object result) {
        Event event = new Event();
        event.eventType = "step_finished";
        event.data = new StepInfo(stepName, stepIndex, "完成", result);
        event.message = "步骤完成: " + stepName;
        return event;
    }

    public static Event planFinished(Object finalResult) {
        Event event = new Event();
        event.eventType = "plan_finished";
        event.data = finalResult;
        event.message = "计划执行完成";
        return event;
    }

    public static Event summaryResult(String summary) {
        Event event = new Event();
        event.eventType = "summary_result";
        event.data = summary;
        event.message = "总结生成完成";
        return event;
    }

    public static Event reportContent(String content) {
        Event event = new Event();
        event.eventType = "report_content";
        event.data = content;
        event.message = "报表内容";
        return event;
    }

    public static Event error(String errorMessage) {
        Event event = new Event();
        event.eventType = "error";
        event.message = errorMessage;
        return event;
    }

    // ========== PlanNotebook 事件构建器 ==========

    public static Event buildFromPlanNotebook(PlanNotebook notebook, Plan plan) {
        if (plan == null) return null;

        SubTask currentTask = null;
        int currentIndex = -1;
        for (int i = 0; i < plan.getSubtasks().size(); i++) {
            SubTask st = plan.getSubtasks().get(i);
            if (st.getState() == SubTaskState.IN_PROGRESS) {
                currentTask = st;
                currentIndex = i;
                break;
            }
        }

        String eventType;
        Object eventData;

        if (currentTask != null && currentIndex >= 0) {
            eventType = "step_started";
            Map<String, Object> data = new HashMap<>();
            data.put("planName", plan.getName());
            data.put("stepIndex", currentIndex);
            data.put("stepName", currentTask.getName());
            data.put("stepDescription", currentTask.getDescription());
            eventData = data;
        } else {
            SubTask lastDone = null;
            int lastDoneIndex = -1;
            for (int i = plan.getSubtasks().size() - 1; i >= 0; i--) {
                SubTask st = plan.getSubtasks().get(i);
                if (st.getState() == SubTaskState.DONE) {
                    lastDone = st;
                    lastDoneIndex = i;
                    break;
                }
            }

            if (lastDone != null) {
                eventType = "step_finished";
                Map<String, Object> data = new HashMap<>();
                data.put("planName", plan.getName());
                data.put("stepIndex", lastDoneIndex);
                data.put("stepName", lastDone.getName());
                data.put("stepOutcome", lastDone.getOutcome());
                eventData = data;
            } else {
                eventType = "plan_created";
                Map<String, Object> data = new HashMap<>();
                data.put("planName", plan.getName());
                data.put("planDescription", plan.getDescription());
                data.put("subtaskCount", plan.getSubtasks().size());
                eventData = data;
            }
        }

        Event event = new Event();
        event.eventType = eventType;
        event.data = eventData;
        event.message = buildMessage(eventType, plan.getName(), eventData);
        return event;
    }

    private static String buildMessage(String eventType, String planName, Object data) {
        if (!(data instanceof Map)) return eventType + ": " + planName;

        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) data;

        return switch (eventType) {
            case "plan_created" -> "计划创建: " + planName + "，共" + dataMap.get("subtaskCount") + "个步骤";
            case "step_started" -> "开始执行: " + dataMap.get("stepName");
            case "step_finished" -> "步骤完成: " + dataMap.get("stepName");
            default -> eventType + ": " + planName;
        };
    }
}
