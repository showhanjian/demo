package agent.util;

import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.formatter.openai.dto.OpenAIMessage;
import io.agentscope.core.message.Msg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义 Formatter，合并多条 system 消息为一条
 * 解决 MiniMax API "user name must be consistent (2013)" 错误
 *
 * 问题原因：
 * 1. SkillBox 生成 system 消息（Available Skills）
 * 2. ReActAgent 的 sysPrompt 生成另一条 system 消息（Agent 专属提示）
 * 3. 两条 system 消息被分别设置 name 字段，导致 MiniMax 报错
 *
 * 解决方案：
 * 1. 合并多条 system 消息为一条
 * 2. 确保 system 消息没有 name 字段
 * 3. 所有 user 消息使用固定的 user name
 */
public class MergedSystemFormatter extends OpenAIChatFormatter {

    private static final String USER_NAME = "user01";

    @Override
    protected List<OpenAIMessage> doFormat(List<Msg> messages) {
        // 调用父类方法进行基本转换
        List<OpenAIMessage> formatted = super.doFormat(messages);

        // 合并多条 system 消息并清理 name 字段
        return mergeAndCleanMessages(formatted);
    }

    /**
     * 合并多条 system 消息，并确保 name 字段一致
     */
    private List<OpenAIMessage> mergeAndCleanMessages(List<OpenAIMessage> messages) {
        List<OpenAIMessage> systemMsgs = new ArrayList<>();
        List<OpenAIMessage> otherMsgs = new ArrayList<>();

        // 分离 system 消息和其他消息
        for (OpenAIMessage msg : messages) {
            if ("system".equals(msg.getRole())) {
                systemMsgs.add(msg);
            } else {
                otherMsgs.add(msg);
            }
        }

        // 构建合并后的消息列表
        List<OpenAIMessage> result = new ArrayList<>();

        // 合并 system 消息（不设置 name 字段）
        if (!systemMsgs.isEmpty()) {
            String mergedContent;
            if (systemMsgs.size() == 1) {
                mergedContent = systemMsgs.get(0).getContentAsString();
            } else {
                mergedContent = systemMsgs.stream()
                    .map(OpenAIMessage::getContentAsString)
                    .collect(Collectors.joining("\n\n"));
            }

            // system 消息不设置 name 字段
            result.add(OpenAIMessage.builder()
                .role("system")
                .content(mergedContent)
                .build());
        }

        // 处理其他消息，统一 name 为 USER_NAME
        for (OpenAIMessage msg : otherMsgs) {
            if ("user".equals(msg.getRole())) {
                // user 消息使用固定的 name
                result.add(OpenAIMessage.builder()
                    .role("user")
                    .content(msg.getContentAsString())
                    .name(USER_NAME)
                    .build());
            } else {
                // 其他消息（assistant, tool）清除 name 字段
                result.add(OpenAIMessage.builder()
                    .role(msg.getRole())
                    .content(msg.getContentAsString())
                    .name(null)  // 清除 name 字段
                    .toolCalls(msg.getToolCalls())
                    .toolCallId(msg.getToolCallId())
                    .build());
            }
        }

        return result;
    }
}
