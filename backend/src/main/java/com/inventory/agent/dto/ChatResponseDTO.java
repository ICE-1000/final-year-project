package com.inventory.agent.dto;

import java.util.List;

public class ChatResponseDTO {
    private String reply;
    private List<String> toolsUsed;

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public List<String> getToolsUsed() { return toolsUsed; }
    public void setToolsUsed(List<String> toolsUsed) { this.toolsUsed = toolsUsed; }
}
