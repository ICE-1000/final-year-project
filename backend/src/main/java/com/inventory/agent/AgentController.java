package com.inventory.agent;

import com.inventory.agent.dto.ChatRequestDTO;
import com.inventory.agent.dto.ChatResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        return ResponseEntity.ok(agentService.chat(request));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("configured", agentService.isConfigured()));
    }
}
