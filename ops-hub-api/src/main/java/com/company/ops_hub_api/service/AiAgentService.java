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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    private final AiAskService aiAskService;

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
        AiAskResponseDTO response = aiAskService.ask(
                new AiAskRequestDTO(
                        request.getMessage(),
                        request.getConversationId(),
                        request.getCurrentPage(),
                        request.getCurrentModule(),
                        request.getContext()
                ),
                httpRequest
        );

        return AiMessageResponseDTO.builder()
                .conversationId(response.getConversationId())
                .response(response.getResponse())
                .suggestedActions(new ArrayList<>())
                .requiresConfirmation(false)
                .context(getAiContext(request.getCurrentPage(), request.getCurrentModule(), request.getContext()))
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

}
