package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.domain.AiAction;
import com.company.ops_hub_api.domain.AiConversation;
import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.AiAgentService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI Agent Controller
 * REST endpoints for AI agent interactions
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiAgentService aiAgentService;

    /**
     * Get AI context for current user
     */
    @RequiresPermission("USE_AI_AGENT")
    @GetMapping("/context")
    public ResponseEntity<AiContextDTO> getContext(
            @RequestParam(required = false) String currentPage,
            @RequestParam(required = false) String currentModule,
            @RequestParam(required = false) Map<String, Object> additionalContext) {
        AiContextDTO context = aiAgentService.getAiContext(currentPage, currentModule, additionalContext);
        return ResponseEntity.ok(context);
    }

    /**
     * Process AI message
     */
    @RequiresPermission("USE_AI_AGENT")
    @PostMapping("/message")
    public ResponseEntity<AiMessageResponseDTO> processMessage(
            @Valid @RequestBody AiMessageRequestDTO request,
            HttpServletRequest httpRequest) {
        AiMessageResponseDTO response = aiAgentService.processMessage(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute AI-suggested action
     */
    @RequiresPermission("USE_AI_AGENT")
    @PostMapping("/action")
    public ResponseEntity<AiActionResponseDTO> executeAction(
            @Valid @RequestBody AiActionRequestDTO request,
            HttpServletRequest httpRequest) {
        AiActionResponseDTO response = aiAgentService.executeAction(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's AI conversations
     */
    @RequiresPermission("USE_AI_AGENT")
    @GetMapping("/conversations")
    public ResponseEntity<List<AiConversation>> getConversations() {
        List<AiConversation> conversations = aiAgentService.getUserConversations();
        return ResponseEntity.ok(conversations);
    }

    /**
     * Get actions for a conversation
     */
    @RequiresPermission("USE_AI_AGENT")
    @GetMapping("/conversations/{conversationId}/actions")
    public ResponseEntity<List<AiAction>> getConversationActions(
            @PathVariable String conversationId) {
        List<AiAction> actions = aiAgentService.getConversationActions(conversationId);
        return ResponseEntity.ok(actions);
    }
}
