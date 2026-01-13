package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.AiAction;
import com.company.ops_hub_api.domain.AiConversation;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.repository.AiActionRepository;
import com.company.ops_hub_api.repository.AiConversationRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Agent Service
 * Handles AI conversations and actions with strict permission checks and guardrails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentService {

    private final AiConversationRepository conversationRepository;
    private final AiActionRepository actionRepository;
    private final UserRepository userRepository;
    private final AiActionValidator actionValidator;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // Store confirmation tokens for restricted actions (in-memory, could be moved to Redis in production)
    private final Map<String, ConfirmationToken> confirmationTokens = new ConcurrentHashMap<>();

    /**
     * Get AI context for current user
     */
    @Transactional(readOnly = true)
    public AiContextDTO getAiContext(String currentPage, String currentModule, Map<String, Object> additionalContext) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            throw new AccessDeniedException("User not authenticated");
        }

        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return AiContextDTO.builder()
                .userId(userId)
                .employeeId(user.getEmployeeId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(new ArrayList<>(userPrincipal.getRoles()))
                .permissions(new ArrayList<>(userPrincipal.getPermissions()))
                .currentPage(currentPage)
                .currentModule(currentModule)
                .additionalContext(additionalContext != null ? additionalContext : new HashMap<>())
                .build();
    }

    /**
     * Process AI message and return response
     * This is where AI LLM integration would happen
     */
    @Transactional
    public AiMessageResponseDTO processMessage(AiMessageRequestDTO request, HttpServletRequest httpRequest) {
        // Check permission to use AI agent
        checkAiAgentPermission();

        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get or create conversation
        AiConversation conversation = getOrCreateConversation(request.getConversationId(), user);

        // Get AI context
        AiContextDTO context = getAiContext(request.getCurrentPage(), request.getCurrentModule(), request.getContext());

        // TODO: Integrate with actual AI/LLM service here
        // For now, we simulate AI response
        String aiResponse = generateAiResponse(request.getMessage(), context);

        // Generate suggested actions based on message and context
        List<AiActionSuggestionDTO> suggestedActions = generateSuggestedActions(request.getMessage(), context);

        // Update conversation context
        updateConversationContext(conversation, request, context);

        // Log conversation
        logAiConversation(conversation, request.getMessage(), aiResponse, httpRequest);

        return AiMessageResponseDTO.builder()
                .conversationId(conversation.getConversationId())
                .response(aiResponse)
                .suggestedActions(suggestedActions)
                .requiresConfirmation(false)
                .context(context)
                .build();
    }

    /**
     * Execute an AI-suggested action
     */
    @Transactional
    public AiActionResponseDTO executeAction(AiActionRequestDTO request, HttpServletRequest httpRequest) {
        // Check permission to use AI agent
        checkAiAgentPermission();

        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validate action permission
        actionValidator.validateActionPermission(request.getActionType(), request.getActionName());

        // Check if confirmation is required
        boolean requiresConfirmation = actionValidator.requiresConfirmation(
                request.getActionType(), request.getActionName());

        if (requiresConfirmation) {
            // Validate confirmation token
            if (request.getConfirmationToken() == null || 
                !validateConfirmationToken(request.getConfirmationToken(), request.getActionType(), request.getActionName())) {
                String confirmationToken = generateConfirmationToken(request.getActionType(), request.getActionName(), user.getId());
                return AiActionResponseDTO.builder()
                        .actionType(request.getActionType())
                        .actionName(request.getActionName())
                        .executionStatus("PENDING")
                        .requiresConfirmation(true)
                        .confirmationToken(confirmationToken)
                        .build();
            }
        }

        // Get or create conversation
        AiConversation conversation = null;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findByConversationId(request.getConversationId())
                    .orElse(null);
        }
        if (conversation == null) {
            conversation = createNewConversation(user, "AI Action: " + request.getActionName());
        }

        // Create AI action record
        AiAction action = new AiAction();
        action.setConversation(conversation);
        action.setActionType(request.getActionType());
        action.setActionName(request.getActionName());
        action.setExecutionStatus("EXECUTING");
        action.setExecutedAt(LocalDateTime.now());

        try {
            // Convert action data to JSON
            if (request.getActionData() != null) {
                action.setActionData(objectMapper.writeValueAsString(request.getActionData()));
            }

            action = actionRepository.save(action);

            // Execute the action (delegate to appropriate service)
            Object result = executeActionInternal(request.getActionType(), request.getActionName(), 
                    request.getActionData(), user);

            // Update action with result
            action.setExecutionStatus("COMPLETED");
            action.setCompletedAt(LocalDateTime.now());
            if (result != null) {
                action.setResultData(objectMapper.writeValueAsString(result));
            }
            action = actionRepository.save(action);

            // Log to audit log
            Map<String, Object> actionData = new HashMap<>();
            actionData.put("actionId", action.getId());
            actionData.put("actionType", request.getActionType());
            actionData.put("actionName", request.getActionName());
            actionData.put("triggeredBy", "AI_AGENT");
            actionData.put("conversationId", conversation.getConversationId());
            if (result != null) {
                actionData.put("result", result);
            }

            auditLogService.logAction("AI_ACTION_EXECUTED", "AI_ACTION", action.getId(), 
                    null, actionData, httpRequest);

            log.info("AI action executed successfully: {} by user {}", request.getActionName(), user.getEmployeeId());

            return AiActionResponseDTO.builder()
                    .actionId(action.getId())
                    .actionType(request.getActionType())
                    .actionName(request.getActionName())
                    .executionStatus("COMPLETED")
                    .resultData(result)
                    .requiresConfirmation(false)
                    .build();

        } catch (AccessDeniedException e) {
            action.setExecutionStatus("FAILED");
            action.setErrorMessage("Permission denied: " + e.getMessage());
            action.setCompletedAt(LocalDateTime.now());
            actionRepository.save(action);

            auditLogService.logError("AI_ACTION_DENIED", "AI_ACTION", action.getId(), 
                    "Permission denied: " + e.getMessage(), httpRequest);

            throw e;
        } catch (Exception e) {
            action.setExecutionStatus("FAILED");
            action.setErrorMessage(e.getMessage());
            action.setCompletedAt(LocalDateTime.now());
            actionRepository.save(action);

            auditLogService.logError("AI_ACTION_FAILED", "AI_ACTION", action.getId(), 
                    "Error: " + e.getMessage(), httpRequest);

            log.error("AI action execution failed: {}", request.getActionName(), e);
            throw new RuntimeException("Failed to execute AI action: " + e.getMessage(), e);
        }
    }

    /**
     * Get user's AI conversations
     */
    @Transactional(readOnly = true)
    public List<AiConversation> getUserConversations() {
        checkAiAgentPermission();
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return conversationRepository.findActiveConversationsByUserId(userPrincipal.getUserId());
    }

    /**
     * Get actions for a conversation
     */
    @Transactional(readOnly = true)
    public List<AiAction> getConversationActions(String conversationId) {
        checkAiAgentPermission();
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        
        AiConversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Verify conversation belongs to user
        if (!conversation.getUser().getId().equals(userPrincipal.getUserId())) {
            throw new AccessDeniedException("Access denied to this conversation");
        }

        return actionRepository.findByConversationId(conversation.getId());
    }

    // Private helper methods

    private void checkAiAgentPermission() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        if (!userPrincipal.hasPermission("USE_AI_AGENT")) {
            throw new AccessDeniedException("Insufficient permission to use AI agent");
        }
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }

    private AiConversation getOrCreateConversation(String conversationId, User user) {
        if (conversationId != null && !conversationId.isEmpty()) {
            return conversationRepository.findByConversationId(conversationId)
                    .orElseGet(() -> createNewConversation(user, "AI Conversation"));
        }
        return createNewConversation(user, "AI Conversation");
    }

    private AiConversation createNewConversation(User user, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setUser(user);
        conversation.setConversationId(UUID.randomUUID().toString());
        conversation.setTitle(title);
        conversation.setStatus("ACTIVE");
        conversation.setModelName("default"); // TODO: Configure model name
        return conversationRepository.save(conversation);
    }

    private void updateConversationContext(AiConversation conversation, AiMessageRequestDTO request, AiContextDTO context) {
        try {
            Map<String, Object> contextData = new HashMap<>();
            contextData.put("currentPage", request.getCurrentPage());
            contextData.put("currentModule", request.getCurrentModule());
            contextData.put("userContext", context);
            contextData.put("lastMessage", request.getMessage());
            contextData.put("updatedAt", LocalDateTime.now().toString());

            conversation.setContextData(objectMapper.writeValueAsString(contextData));
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
        } catch (Exception e) {
            log.error("Error updating conversation context", e);
        }
    }

    private String generateAiResponse(String message, AiContextDTO context) {
        // TODO: Integrate with actual AI/LLM service
        // This is a placeholder that simulates AI response
        return String.format(
                "I understand you're asking about: %s. " +
                "Based on your role (%s) and permissions, I can help you with that. " +
                "Here are some actions I can perform for you.",
                message,
                String.join(", ", context.getRoles())
        );
    }

    private List<AiActionSuggestionDTO> generateSuggestedActions(String message, AiContextDTO context) {
        List<AiActionSuggestionDTO> suggestions = new ArrayList<>();

        // Generate suggestions based on user permissions and message content
        if (context.getPermissions().contains("VIEW_CUSTOMERS")) {
            if (message.toLowerCase().contains("customer") || message.toLowerCase().contains("client")) {
                suggestions.add(AiActionSuggestionDTO.builder()
                        .actionType("DATA_QUERY")
                        .actionName("VIEW_CUSTOMERS")
                        .description("View customer information")
                        .requiresPermission(true)
                        .requiredPermission("VIEW_CUSTOMERS")
                        .requiresConfirmation(false)
                        .build());
            }
        }

        if (context.getPermissions().contains("VIEW_REPORTS")) {
            if (message.toLowerCase().contains("report") || message.toLowerCase().contains("data")) {
                suggestions.add(AiActionSuggestionDTO.builder()
                        .actionType("REPORT_GENERATION")
                        .actionName("GENERATE_REPORT")
                        .description("Generate a report")
                        .requiresPermission(true)
                        .requiredPermission("VIEW_REPORTS")
                        .requiresConfirmation(false)
                        .build());
            }
        }

        return suggestions;
    }

    private Object executeActionInternal(String actionType, String actionName, 
                                        Map<String, Object> actionData, User user) {
        // TODO: Delegate to appropriate service based on action type
        // This is a placeholder - actual implementation would route to specific services
        
        switch (actionType) {
            case "DATA_QUERY":
                // Delegate to appropriate data service
                return executeDataQuery(actionName, actionData, user);
            case "REPORT_GENERATION":
                // Delegate to report service
                return executeReportGeneration(actionName, actionData, user);
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
    }

    private Object executeDataQuery(String actionName, Map<String, Object> actionData, User user) {
        // TODO: Implement actual data query logic
        return Map.of("status", "success", "message", "Data query executed", "action", actionName);
    }

    private Object executeReportGeneration(String actionName, Map<String, Object> actionData, User user) {
        // TODO: Implement actual report generation logic
        return Map.of("status", "success", "message", "Report generated", "action", actionName);
    }

    private String generateConfirmationToken(String actionType, String actionName, Long userId) {
        String token = UUID.randomUUID().toString();
        confirmationTokens.put(token, new ConfirmationToken(actionType, actionName, LocalDateTime.now()));
        return token;
    }

    private boolean validateConfirmationToken(String token, String actionType, String actionName) {
        ConfirmationToken confirmationToken = confirmationTokens.get(token);
        if (confirmationToken == null) {
            return false;
        }

        // Check if token is expired (5 minutes)
        if (confirmationToken.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            confirmationTokens.remove(token);
            return false;
        }

        // Verify action matches
        if (!confirmationToken.getActionType().equals(actionType) || 
            !confirmationToken.getActionName().equals(actionName)) {
            return false;
        }

        // Remove token after validation (one-time use)
        confirmationTokens.remove(token);
        return true;
    }

    private void logAiConversation(AiConversation conversation, String message, String response, HttpServletRequest request) {
        try {
            Map<String, Object> conversationData = new HashMap<>();
            conversationData.put("conversationId", conversation.getConversationId());
            conversationData.put("message", message);
            conversationData.put("response", response);
            conversationData.put("messageLength", message.length());
            conversationData.put("responseLength", response.length());

            auditLogService.logAction("AI_CONVERSATION", "AI_CONVERSATION", conversation.getId(), 
                    null, conversationData, request);
        } catch (Exception e) {
            log.error("Error logging AI conversation", e);
        }
    }

    // Inner class for confirmation tokens
    private static class ConfirmationToken {
        private final String actionType;
        private final String actionName;
        private final LocalDateTime createdAt;

        public ConfirmationToken(String actionType, String actionName, LocalDateTime createdAt) {
            this.actionType = actionType;
            this.actionName = actionName;
            this.createdAt = createdAt;
        }

        public String getActionType() { return actionType; }
        public String getActionName() { return actionName; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
