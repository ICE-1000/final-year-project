package com.inventory.agent.deepseek;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepSeekMessage {
    private String role;
    private String content;

    @JsonProperty("tool_calls")
    private List<DeepSeekToolCall> toolCalls;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    public DeepSeekMessage() {
    }

    public static DeepSeekMessage system(String content) {
        DeepSeekMessage m = new DeepSeekMessage();
        m.role = "system";
        m.content = content;
        return m;
    }

    public static DeepSeekMessage user(String content) {
        DeepSeekMessage m = new DeepSeekMessage();
        m.role = "user";
        m.content = content;
        return m;
    }

    public static DeepSeekMessage assistant(String content) {
        DeepSeekMessage m = new DeepSeekMessage();
        m.role = "assistant";
        m.content = content;
        return m;
    }

    public static DeepSeekMessage tool(String toolCallId, String content) {
        DeepSeekMessage m = new DeepSeekMessage();
        m.role = "tool";
        m.toolCallId = toolCallId;
        m.content = content;
        return m;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<DeepSeekToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<DeepSeekToolCall> toolCalls) { this.toolCalls = toolCalls; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
}
