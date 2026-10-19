package com.inventory.agent.deepseek;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepSeekToolCall {
    private String id;
    private String type = "function";
    private DeepSeekFunctionCall function;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public DeepSeekFunctionCall getFunction() { return function; }
    public void setFunction(DeepSeekFunctionCall function) { this.function = function; }
}
