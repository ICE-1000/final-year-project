package com.inventory.agent.deepseek;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {
    private String model;
    private List<DeepSeekMessage> messages;
    private List<DeepSeekToolDefinition> tools;

    @JsonProperty("tool_choice")
    private String toolChoice = "auto";

    private Double temperature = 0.2;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<DeepSeekMessage> getMessages() { return messages; }
    public void setMessages(List<DeepSeekMessage> messages) { this.messages = messages; }
    public List<DeepSeekToolDefinition> getTools() { return tools; }
    public void setTools(List<DeepSeekToolDefinition> tools) { this.tools = tools; }
    public String getToolChoice() { return toolChoice; }
    public void setToolChoice(String toolChoice) { this.toolChoice = toolChoice; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
}
