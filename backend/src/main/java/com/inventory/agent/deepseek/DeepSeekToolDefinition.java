package com.inventory.agent.deepseek;

public class DeepSeekToolDefinition {
    private String type = "function";
    private DeepSeekFunctionDef function;

    public DeepSeekToolDefinition() {
    }

    public DeepSeekToolDefinition(DeepSeekFunctionDef function) {
        this.function = function;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public DeepSeekFunctionDef getFunction() { return function; }
    public void setFunction(DeepSeekFunctionDef function) { this.function = function; }
}
