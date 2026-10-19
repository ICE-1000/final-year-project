package com.inventory.agent.deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ChatCompletionResponse {
    private List<Choice> choices;

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public static class Choice {
        private DeepSeekMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;

        public DeepSeekMessage getMessage() { return message; }
        public void setMessage(DeepSeekMessage message) { this.message = message; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }
}
