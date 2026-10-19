package com.inventory.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.agent.deepseek.ChatCompletionRequest;
import com.inventory.agent.deepseek.ChatCompletionResponse;
import com.inventory.agent.deepseek.DeepSeekClient;
import com.inventory.agent.deepseek.DeepSeekMessage;
import com.inventory.agent.deepseek.DeepSeekToolCall;
import com.inventory.agent.dto.ChatRequestDTO;
import com.inventory.agent.dto.ChatResponseDTO;
import com.inventory.agent.dto.ChatTurnDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentService {
    private static final int MAX_TOOL_ITERATIONS = 5;
    private static final int MAX_HISTORY_TURNS = 20;

    private final DeepSeekClient deepSeekClient;
    private final AgentToolRegistry toolRegistry;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AgentService(DeepSeekClient deepSeekClient, AgentToolRegistry toolRegistry,
                         UserService userService, ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.toolRegistry = toolRegistry;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return deepSeekClient.isConfigured();
    }

    public ChatResponseDTO chat(ChatRequestDTO request) {
        if (!deepSeekClient.isConfigured()) {
            throw new BadRequestException(
                    "The AI assistant is not configured yet - ask an administrator to set DEEPSEEK_API_KEY.");
        }

        User currentUser = userService.getCurrentUser();
        List<AgentTool> tools = toolRegistry.toolsForRole(currentUser.getRole());

        List<DeepSeekMessage> messages = new ArrayList<>();
        messages.add(DeepSeekMessage.system(buildSystemPrompt(currentUser)));

        if (request.getHistory() != null) {
            List<ChatTurnDTO> trimmed = request.getHistory().size() > MAX_HISTORY_TURNS
                    ? request.getHistory().subList(request.getHistory().size() - MAX_HISTORY_TURNS, request.getHistory().size())
                    : request.getHistory();
            for (ChatTurnDTO turn : trimmed) {
                if (turn.getContent() == null || turn.getContent().isBlank()) {
                    continue;
                }
                messages.add("assistant".equals(turn.getRole())
                        ? DeepSeekMessage.assistant(turn.getContent())
                        : DeepSeekMessage.user(turn.getContent()));
            }
        }
        messages.add(DeepSeekMessage.user(request.getMessage()));

        List<String> toolsUsed = new ArrayList<>();
        String finalReply = null;

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            ChatCompletionRequest completionRequest = new ChatCompletionRequest();
            completionRequest.setModel(deepSeekClient.getModel());
            completionRequest.setMessages(messages);
            completionRequest.setTools(tools.stream().map(AgentTool::toDefinition).collect(Collectors.toList()));

            ChatCompletionResponse response;
            try {
                response = deepSeekClient.complete(completionRequest);
            } catch (RestClientException ex) {
                throw new BadRequestException("The AI assistant is temporarily unavailable. Please try again shortly.");
            }

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new BadRequestException("The AI assistant returned an empty response. Please try again.");
            }

            DeepSeekMessage assistantMessage = response.getChoices().get(0).getMessage();
            messages.add(assistantMessage);

            if (assistantMessage.getToolCalls() == null || assistantMessage.getToolCalls().isEmpty()) {
                finalReply = assistantMessage.getContent();
                break;
            }

            for (DeepSeekToolCall call : assistantMessage.getToolCalls()) {
                String toolName = call.getFunction() == null ? null : call.getFunction().getName();
                toolsUsed.add(toolName == null ? "unknown" : toolName);
                messages.add(DeepSeekMessage.tool(call.getId(), executeTool(toolName, call, currentUser)));
            }
        }

        if (finalReply == null || finalReply.isBlank()) {
            finalReply = "I wasn't able to finish that within the allowed number of steps - "
                    + "please try rephrasing or breaking the request into smaller parts.";
        }

        ChatResponseDTO out = new ChatResponseDTO();
        out.setReply(finalReply);
        out.setToolsUsed(toolsUsed);
        return out;
    }

    private String executeTool(String toolName, DeepSeekToolCall call, User currentUser) {
        try {
            AgentTool tool = toolName == null ? null : toolRegistry.find(toolName).orElse(null);
            if (tool == null) {
                return writeError("Unknown tool: " + toolName);
            }
            if (!tool.getAllowedRoles().contains(currentUser.getRole())) {
                return writeError("That action isn't permitted for your role.");
            }
            String rawArgs = call.getFunction().getArguments();
            JsonNode args = objectMapper.readTree(rawArgs == null || rawArgs.isBlank() ? "{}" : rawArgs);
            Object result = tool.getExecutor().execute(args, currentUser);
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return writeError(ex.getMessage() == null ? "Unexpected error." : ex.getMessage());
        }
    }

    private String writeError(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (Exception ex) {
            return "{\"error\":\"Unexpected error.\"}";
        }
    }

    private String buildSystemPrompt(User user) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("username", user.getUsername());
        context.put("role", user.getRole().name());
        if (user.getDepartment() != null) {
            context.put("department", user.getDepartment().getDepartmentName());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("You are the built-in assistant for the UNZA Inventory Management System (IMS), a Spring Boot ")
          .append("application that tracks inventory by category, with brand and specification distinguishing ")
          .append("items within a category. Every physical item gets its own individually numbered barcode at ")
          .append("registration; registering the same category + name + brand + specification again adds more ")
          .append("units to that same item instead of duplicating it. Inventory requests submitted by departments ")
          .append("go through PENDING -> APPROVED/REJECTED. Allocating inventory pulls specific available units ")
          .append("and assigns them to a department.\n\n")
          .append("You are speaking with: ").append(context).append(".\n\n")
          .append("Rules:\n")
          .append("- Use the available tools for anything requiring current data - never guess or make up ")
          .append("quantities, statuses, names, or ids. If a tool call fails or returns an error, tell the user ")
          .append("plainly what went wrong; do not pretend it succeeded.\n")
          .append("- Before performing any action that changes data (registering inventory, creating a category, ")
          .append("allocating stock, submitting a request, approving or rejecting a request), make sure the ")
          .append("user's message clearly specifies what's needed. If something required is missing or ambiguous, ")
          .append("ask a clarifying question instead of guessing at it.\n")
          .append("- Only use the tools available to you - if the user asks for something outside your permitted ")
          .append("tools (for example a DEPARTMENT user asking to approve a request or add inventory), explain ")
          .append("plainly that this requires ADMIN access and isn't something you can do for them.\n")
          .append("- Keep answers concise and professional. Use plain text, not markdown tables.\n");

        if (user.getRole() == Role.DEPARTMENT) {
            sb.append("- You may only ever act on behalf of this user's own department (")
              .append(context.getOrDefault("department", "none")).append(") - you have no ability to see or ")
              .append("affect any other department's data.\n");
        }

        return sb.toString();
    }
}
