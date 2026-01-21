package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.AiAction;
import com.company.ops_hub_api.domain.AiConversation;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.AiAskRequestDTO;
import com.company.ops_hub_api.dto.AiAskResponseDTO;
import com.company.ops_hub_api.repository.AiActionRepository;
import com.company.ops_hub_api.repository.AiConversationRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.HierarchyUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAskService {

    private static final String MESSAGE_ACTION_TYPE = "MESSAGE";
    private static final int PROMPT_HISTORY_LIMIT = 20;

    private final AiConversationRepository conversationRepository;
    private final AiActionRepository actionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final ReportDataFilter dataFilter;
    private final AiIntentDetector intentDetector;
    private final AiPromptBuilder promptBuilder;
    private final OllamaClient ollamaClient;

    @Transactional
    public AiAskResponseDTO ask(AiAskRequestDTO request, HttpServletRequest httpRequest) {
        checkAiAgentPermission();
        User user = getCurrentUser();

        AiConversation conversation = getOrCreateConversation(request.getConversationId(), user, request.getQuestion());
        updateConversationContext(conversation, request);

        AiIntent intent = intentDetector.detect(request.getQuestion());
        String responseText;

        if (intent == AiIntent.UNKNOWN) {
            responseText = "I can help with pending payments, agent performance, or visit summaries. "
                    + "Please rephrase your request.";
        } else {
            String businessSummary = resolveBusinessSummary(user, intent);
            List<Map<String, String>> recentMessages = getRecentMessages(conversation.getId());
            String prompt = promptBuilder.buildPrompt(user, request.getQuestion(), intent, businessSummary, recentMessages);
            responseText = ollamaClient.generate(prompt);
            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Ollama response empty; returning authorized summary fallback.");
                responseText = businessSummary + " (AI engine unavailable; showing authorized summary.)";
            }
        }

        responseText = sanitizeAiResponse(responseText);

        storeMessage(conversation, "USER", request.getQuestion());
        storeMessage(conversation, "AI", responseText);

        Map<String, Object> auditData = new HashMap<>();
        auditData.put("conversationId", conversation.getConversationId());
        auditData.put("intent", intent.name());
        auditData.put("questionLength", request.getQuestion() != null ? request.getQuestion().length() : 0);
        auditData.put("responseLength", responseText != null ? responseText.length() : 0);
        auditLogService.logAction("AI_ASK", "AI_CONVERSATION", conversation.getId(), null, auditData, httpRequest);

        return new AiAskResponseDTO(conversation.getConversationId(), responseText);
    }

    private String resolveBusinessSummary(User user, AiIntent intent) {
        return switch (intent) {
            case PENDING_PAYMENTS_SUMMARY -> buildPendingPaymentsSummary(user);
            case VISIT_SUMMARY -> buildVisitSummary(user);
            case AGENT_PERFORMANCE -> buildAgentPerformanceSummary(user);
            default -> "No authorized data available.";
        };
    }

    private String buildPendingPaymentsSummary(User user) {
        String accessClause = dataFilter.buildAccessControlWhereClause(user, "c");
        String sql = "SELECT COUNT(1) AS pendingCount, COALESCE(SUM(p.amount),0) AS pendingAmount " +
                "FROM payments p JOIN customers c ON c.id = p.customer_id " +
                "WHERE p.payment_status = :status" + accessClause;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("status", "INITIATED");
        Object[] row = (Object[]) query.getSingleResult();
        long count = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        BigDecimal amount = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
        return String.format("Pending payments in scope: %d; total amount: %s.", count, amount);
    }

    private String buildVisitSummary(User user) {
        String accessClause = dataFilter.buildAccessControlWhereClause(user, "c");
        String sql = "SELECT COUNT(1) AS totalVisits, " +
                "SUM(CASE WHEN v.visit_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedVisits, " +
                "SUM(CASE WHEN v.visit_status = 'PENDING' THEN 1 ELSE 0 END) AS pendingVisits " +
                "FROM customer_visits v JOIN customers c ON c.id = v.customer_id " +
                "WHERE 1=1" + accessClause;
        Query query = entityManager.createNativeQuery(sql);
        Object[] row = (Object[]) query.getSingleResult();
        long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long completed = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        long pending = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        return String.format("Visits in scope: total %d, completed %d, pending %d.", total, completed, pending);
    }

    private String buildAgentPerformanceSummary(User user) {
        String userType = HierarchyUtil.normalizeUserType(user);
        Map<String, Object> scopeParams = new HashMap<>();
        String scopeClause = buildUserScopeClause(userType, user, scopeParams);

        String agentsSql = "SELECT COUNT(1) FROM users u " +
                "JOIN areas a ON u.area_id = a.id " +
                "JOIN zones z ON a.zone_id = z.id " +
                "JOIN circles ci ON z.circle_id = ci.id " +
                "JOIN clusters cl ON ci.cluster_id = cl.id " +
                "WHERE u.user_type = :agentType AND u.active = 1" + scopeClause;
        Query agentsQuery = entityManager.createNativeQuery(agentsSql);
        agentsQuery.setParameter("agentType", "AGENT");
        scopeParams.forEach(agentsQuery::setParameter);
        long agentCount = ((Number) agentsQuery.getSingleResult()).longValue();

        String accessClause = dataFilter.buildAccessControlWhereClause(user, "c");
        String allocSql = "SELECT COUNT(1) FROM customer_allocations ca " +
                "JOIN customers c ON c.id = ca.customer_id " +
                "WHERE ca.status = 'ACTIVE' AND ca.role_code = 'AGENT'" + accessClause;
        Query allocQuery = entityManager.createNativeQuery(allocSql);
        long allocationCount = ((Number) allocQuery.getSingleResult()).longValue();

        String visitSql = "SELECT COUNT(1) FROM customer_visits v " +
                "JOIN users u ON u.id = v.user_id " +
                "JOIN customers c ON c.id = v.customer_id " +
                "WHERE u.user_type = 'AGENT'" + accessClause;
        Query visitQuery = entityManager.createNativeQuery(visitSql);
        long visitCount = ((Number) visitQuery.getSingleResult()).longValue();

        String paymentSql = "SELECT COUNT(1) FROM payments p " +
                "JOIN users u ON u.id = p.user_id " +
                "JOIN customers c ON c.id = p.customer_id " +
                "WHERE u.user_type = 'AGENT' AND p.payment_status = 'SUCCESS'" + accessClause;
        Query paymentQuery = entityManager.createNativeQuery(paymentSql);
        long paymentCount = ((Number) paymentQuery.getSingleResult()).longValue();

        double avgVisits = agentCount > 0 ? (double) visitCount / agentCount : 0.0;
        return String.format("Agents in scope: %d; active allocations: %d; visits logged: %d; " +
                        "successful payments: %d; average visits per agent: %.2f.",
                agentCount, allocationCount, visitCount, paymentCount, avgVisits);
    }

    private String buildUserScopeClause(String userType, User user, Map<String, Object> params) {
        if (HierarchyUtil.ADMIN.equals(userType)) {
            return "";
        }
        if (HierarchyUtil.CLUSTER_HEAD.equals(userType)) {
            params.put("clusterId", HierarchyUtil.getClusterId(user));
            return " AND cl.id = :clusterId";
        }
        if (HierarchyUtil.CIRCLE_HEAD.equals(userType)) {
            params.put("circleId", HierarchyUtil.getCircleId(user));
            return " AND ci.id = :circleId";
        }
        if (HierarchyUtil.ZONE_HEAD.equals(userType)) {
            params.put("zoneId", HierarchyUtil.getZoneId(user));
            return " AND z.id = :zoneId";
        }
        if (HierarchyUtil.AREA_HEAD.equals(userType) || HierarchyUtil.STORE_HEAD.equals(userType)) {
            params.put("areaId", HierarchyUtil.getAreaId(user));
            return " AND a.id = :areaId";
        }
        if (HierarchyUtil.AGENT.equals(userType)) {
            params.put("userId", user.getId());
            return " AND u.id = :userId";
        }
        return " AND 1=0";
    }

    private List<Map<String, String>> getRecentMessages(Long conversationId) {
        List<AiAction> actions = actionRepository.findRecentByConversationAndActionType(
                conversationId, MESSAGE_ACTION_TYPE, PageRequest.of(0, PROMPT_HISTORY_LIMIT));
        Collections.reverse(actions);
        List<Map<String, String>> messages = new ArrayList<>();
        for (AiAction action : actions) {
            try {
                Map<String, Object> data = objectMapper.readValue(
                        action.getActionData(),
                        new TypeReference<Map<String, Object>>() {}
                );
                Object roleValue = data.get("role");
                Object contentValue = data.get("content");
                String role = roleValue != null ? roleValue.toString() : "USER";
                String content = contentValue != null ? contentValue.toString() : "";
                Map<String, String> entry = new HashMap<>();
                entry.put("role", role);
                entry.put("content", content);
                messages.add(entry);
            } catch (Exception e) {
                log.warn("Failed to parse AI message action data", e);
            }
        }
        return messages;
    }

    private void storeMessage(AiConversation conversation, String role, String content) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("role", role);
            data.put("content", content);
            data.put("timestamp", LocalDateTime.now().toString());

            AiAction message = new AiAction();
            message.setConversation(conversation);
            message.setActionType(MESSAGE_ACTION_TYPE);
            message.setActionName(role);
            message.setActionData(objectMapper.writeValueAsString(data));
            message.setExecutionStatus("COMPLETED");
            actionRepository.save(message);
        } catch (Exception e) {
            log.error("Failed to store AI message", e);
        }
    }

    private String sanitizeAiResponse(String response) {
        if (response == null) {
            return null;
        }
        String lowered = response.toLowerCase();
        if (lowered.contains("select ") || lowered.contains("update ") || lowered.contains("delete ")
                || lowered.contains("insert ") || lowered.contains(" from ") || lowered.contains(" join ")) {
            return "I can’t provide SQL or internal query details. I can summarize the results in plain language.";
        }
        return response.trim();
    }

    private void updateConversationContext(AiConversation conversation, AiAskRequestDTO request) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("currentPage", request.getCurrentPage());
            context.put("currentModule", request.getCurrentModule());
            context.put("additionalContext", request.getContext());
            context.put("lastQuestion", request.getQuestion());
            context.put("updatedAt", LocalDateTime.now().toString());
            conversation.setContextData(objectMapper.writeValueAsString(context));
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
        } catch (Exception e) {
            log.error("Failed to update AI conversation context", e);
        }
    }

    private AiConversation getOrCreateConversation(String conversationId, User user, String question) {
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            AiConversation conversation = conversationRepository.findByConversationId(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Access denied to this conversation");
            }
            return conversation;
        }
        AiConversation conversation = new AiConversation();
        conversation.setUser(user);
        conversation.setConversationId(UUID.randomUUID().toString());
        conversation.setTitle(buildTitle(question));
        conversation.setStatus("ACTIVE");
        conversation.setModelName("llama3");
        return conversationRepository.save(conversation);
    }

    private String buildTitle(String question) {
        if (question == null) {
            return "AI Conversation";
        }
        String trimmed = question.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
    }

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

    private User getCurrentUser() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null || userPrincipal.getUserId() == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        Long userId = Objects.requireNonNull(userPrincipal.getUserId(), "User ID cannot be null");
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
