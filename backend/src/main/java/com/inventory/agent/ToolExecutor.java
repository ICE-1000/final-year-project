package com.inventory.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.inventory.model.User;

@FunctionalInterface
public interface ToolExecutor {
    Object execute(JsonNode arguments, User currentUser) throws Exception;
}
