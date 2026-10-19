package com.inventory.agent.dto;

import javax.validation.constraints.NotBlank;
import java.util.List;

public class ChatRequestDTO {
    @NotBlank
    private String message;

    private List<ChatTurnDTO> history;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<ChatTurnDTO> getHistory() { return history; }
    public void setHistory(List<ChatTurnDTO> history) { this.history = history; }
}
