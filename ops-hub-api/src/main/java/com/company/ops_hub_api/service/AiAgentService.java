package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.AiAction;
import com.company.ops_hub_api.domain.AiConversation;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.repository.AiActionRepository;
import com.company.ops_hub_api.repository.AiConversationRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.HierarchyUtil;
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
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

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

        Map<String, Object> context = new HashMap<>();
        if (additionalContext != null) {
            context.putAll(additionalContext);
        }
        context.put("userType", HierarchyUtil.normalizeUserType(user));
        context.put("geography", HierarchyUtil.buildGeographyContext(user));

        return AiContextDTO.builder()
                .userId(userId)
                .employeeId(user.getEmployeeId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(new ArrayList<>(userPrincipal.getRoles()))
                .permissions(new ArrayList<>(userPrincipal.getPermissions()))
                .currentPage(currentPage)
                .currentModule(currentModule)
                .additionalContext(context)
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

        // Actions are disabled per business rules (read & summarize only)
        List<AiActionSuggestionDTO> suggestedActions = new ArrayList<>();

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
        throw new AccessDeniedException("AI actions are disabled. Read & summarize only.");
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
                "Based on your role (%s) and permissions, I can summarize and explain the data you can access.",
                message,
                String.join(", ", context.getRoles())
        );
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

}
