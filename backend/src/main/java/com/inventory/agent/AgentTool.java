package com.inventory.agent;

import com.inventory.agent.deepseek.DeepSeekFunctionDef;
import com.inventory.agent.deepseek.DeepSeekToolDefinition;
import com.inventory.model.Role;

import java.util.Set;

// Defines one assistant tool and its permissions.
public class AgentTool {
    private final String name;
    private final String description;
    private final Object parametersSchema;
    private final Set<Role> allowedRoles;
    private final ToolExecutor executor;

    public AgentTool(String name, String description, Object parametersSchema,
                      Set<Role> allowedRoles, ToolExecutor executor) {
        this.name = name;
        this.description = description;
        this.parametersSchema = parametersSchema;
        this.allowedRoles = allowedRoles;
        this.executor = executor;
    }

    public String getName() { return name; }
    public Set<Role> getAllowedRoles() { return allowedRoles; }
    public ToolExecutor getExecutor() { return executor; }

    public DeepSeekToolDefinition toDefinition() {
        return new DeepSeekToolDefinition(new DeepSeekFunctionDef(name, description, parametersSchema));
    }
}
