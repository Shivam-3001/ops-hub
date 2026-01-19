package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.AppNotification;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.DashboardActivityDTO;
import com.company.ops_hub_api.dto.DashboardCardDTO;
import com.company.ops_hub_api.dto.DashboardMetricDTO;
import com.company.ops_hub_api.dto.DashboardResponseDTO;
import com.company.ops_hub_api.repository.AppNotificationRepository;
import com.company.ops_hub_api.repository.CustomerAllocationRepository;
import com.company.ops_hub_api.repository.CustomerRepository;
import com.company.ops_hub_api.repository.CustomerVisitRepository;
import com.company.ops_hub_api.repository.PaymentRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.HierarchyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final CustomerVisitRepository visitRepository;
    private final CustomerAllocationRepository allocationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AppNotificationRepository notificationRepository;
    private final ReportDataFilter reportDataFilter;

    @Transactional(readOnly = true)
    public DashboardResponseDTO getDashboard() {
        User currentUser = getCurrentUser();
        String userType = HierarchyUtil.normalizeUserType(currentUser);

        Set<Long> accessibleCustomerIds = reportDataFilter.getAccessibleCustomerIds(currentUser);
        List<Long> customerIds = accessibleCustomerIds != null
                ? new ArrayList<>(accessibleCustomerIds)
                : List.of();

        boolean hasCustomerScope = accessibleCustomerIds != null && !customerIds.isEmpty();

        long customerCount = accessibleCustomerIds == null
                ? customerRepository.count()
                : customerIds.size();

        long activeAllocations = accessibleCustomerIds == null
                ? allocationRepository.findByStatus("ACTIVE").size()
                : (hasCustomerScope ? allocationRepository.countActiveByCustomerIds(customerIds) : 0);

        BigDecimal pendingAmount = accessibleCustomerIds == null
                ? customerRepository.sumPendingAmountAll()
                : (hasCustomerScope ? customerRepository.sumPendingAmountByIds(customerIds) : BigDecimal.ZERO);

        long pendingPayments = accessibleCustomerIds == null
                ? paymentRepository.countByPaymentStatusValue("INITIATED")
                : (hasCustomerScope ? paymentRepository.countByPaymentStatusValueAndCustomerIds("INITIATED", customerIds) : 0);

        long successfulPayments = accessibleCustomerIds == null
                ? paymentRepository.countByPaymentStatusValue("SUCCESS")
                : (hasCustomerScope ? paymentRepository.countByPaymentStatusValueAndCustomerIds("SUCCESS", customerIds) : 0);

        BigDecimal collectedAmount = accessibleCustomerIds == null
                ? paymentRepository.sumAmountByStatus("SUCCESS")
                : (hasCustomerScope ? paymentRepository.sumAmountByStatusAndCustomerIds("SUCCESS", customerIds) : BigDecimal.ZERO);

        long visitsCount = accessibleCustomerIds == null
                ? visitRepository.count()
                : (hasCustomerScope ? visitRepository.countByCustomerIds(customerIds) : 0);

        long assignedCustomers = userType.equals(HierarchyUtil.AGENT)
                ? allocationRepository.countActiveByUserId(currentUser.getId())
                : activeAllocations;

        long visitsToday = userType.equals(HierarchyUtil.AGENT)
                ? visitRepository.countByUserIdAndVisitDateBetween(
                        currentUser.getId(),
                        LocalDate.now().atStartOfDay(),
                        LocalDate.now().plusDays(1).atStartOfDay())
                : 0;

        long teamSize = resolveTeamSize(userType, currentUser);

        List<DashboardCardDTO> cards = buildCards(userType, customerCount, assignedCustomers, pendingPayments,
                collectedAmount, visitsCount, visitsToday, teamSize);

        List<DashboardMetricDTO> metrics = buildMetrics(pendingAmount, successfulPayments, pendingPayments, visitsCount);

        List<DashboardActivityDTO> recentActivity = buildRecentActivity(currentUser.getId());

        List<DashboardActivityDTO> alerts = recentActivity.stream()
                .filter(activity -> "ESCALATION".equalsIgnoreCase(activity.getType()))
                .limit(5)
                .collect(Collectors.toList());

        return DashboardResponseDTO.builder()
                .userType(userType)
                .userName(currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername())
                .cards(cards)
                .metrics(metrics)
                .recentActivity(recentActivity)
                .alerts(alerts)
                .build();
    }

    private List<DashboardCardDTO> buildCards(String userType, long customerCount, long assignedCustomers,
                                              long pendingPayments, BigDecimal collectedAmount, long visitsCount,
                                              long visitsToday, long teamSize) {
        List<DashboardCardDTO> cards = new ArrayList<>();

        if (HierarchyUtil.ADMIN.equals(userType)) {
            cards.add(card("users", "Total Users", String.valueOf(teamSize), "Active users", "👥", "bg-blue-100", 4));
            cards.add(card("customers", "Total Customers", String.valueOf(customerCount), "Across all regions", "👤", "bg-indigo-100", null));
            cards.add(card("payments", "Pending Payments", String.valueOf(pendingPayments), "Awaiting collections", "💳", "bg-amber-100", null));
            cards.add(card("collections", "Collected Amount", formatAmount(collectedAmount), "Successful payments", "✅", "bg-green-100", null));
            return cards;
        }

        if (HierarchyUtil.AGENT.equals(userType)) {
            cards.add(card("assigned", "Assigned Customers", String.valueOf(assignedCustomers), "Your active list", "📌", "bg-blue-100", null));
            cards.add(card("visits", "Visits Today", String.valueOf(visitsToday), "Completed today", "🧭", "bg-emerald-100", null));
            cards.add(card("pending", "Pending Payments", String.valueOf(pendingPayments), "Customers awaiting payment", "💳", "bg-amber-100", null));
            cards.add(card("collections", "Collected Amount", formatAmount(collectedAmount), "Your collections", "✅", "bg-green-100", null));
            return cards;
        }

        cards.add(card("customers", "Customers in Scope", String.valueOf(customerCount), "Assigned to your region", "👤", "bg-indigo-100", null));
        cards.add(card("team", "Team Members", String.valueOf(teamSize), "Active in your scope", "👥", "bg-blue-100", null));
        cards.add(card("pending", "Pending Payments", String.valueOf(pendingPayments), "Awaiting collection", "💳", "bg-amber-100", null));
        cards.add(card("visits", "Total Visits", String.valueOf(visitsCount), "Completed visits", "🧭", "bg-emerald-100", null));
        return cards;
    }

    private List<DashboardMetricDTO> buildMetrics(BigDecimal pendingAmount, long successfulPayments,
                                                  long pendingPayments, long visitsCount) {
        List<DashboardMetricDTO> metrics = new ArrayList<>();
        metrics.add(metric("Pending Amount", formatAmount(pendingAmount), "Across accessible customers"));
        metrics.add(metric("Successful Payments", String.valueOf(successfulPayments), "Completed collections"));
        metrics.add(metric("Pending Payments", String.valueOf(pendingPayments), "Awaiting completion"));
        metrics.add(metric("Total Visits", String.valueOf(visitsCount), "All recorded visits"));
        return metrics;
    }

    private List<DashboardActivityDTO> buildRecentActivity(Long userId) {
        List<AppNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .limit(8)
                .map(notification -> DashboardActivityDTO.builder()
                        .title(notification.getTitle())
                        .description(notification.getMessage())
                        .type(notification.getNotificationType())
                        .createdAt(notification.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private long resolveTeamSize(String userType, User currentUser) {
        if (HierarchyUtil.ADMIN.equals(userType)) {
            return userRepository.count();
        }
        if (HierarchyUtil.CLUSTER_HEAD.equals(userType)) {
            Long clusterId = HierarchyUtil.getClusterId(currentUser);
            return clusterId == null ? 0 : userRepository.findByAreaZoneCircleClusterId(clusterId).size();
        }
        if (HierarchyUtil.CIRCLE_HEAD.equals(userType)) {
            Long circleId = HierarchyUtil.getCircleId(currentUser);
            return circleId == null ? 0 : userRepository.findByAreaZoneCircleId(circleId).size();
        }
        if (HierarchyUtil.ZONE_HEAD.equals(userType)) {
            Long zoneId = HierarchyUtil.getZoneId(currentUser);
            return zoneId == null ? 0 : userRepository.findByAreaZoneId(zoneId).size();
        }
        if (HierarchyUtil.AREA_HEAD.equals(userType) || HierarchyUtil.STORE_HEAD.equals(userType)) {
            Long areaId = HierarchyUtil.getAreaId(currentUser);
            return areaId == null ? 0 : userRepository.findByAreaId(areaId).size();
        }
        return 1;
    }

    private DashboardCardDTO card(String id, String title, String value, String subtitle,
                                  String icon, String color, Integer trend) {
        return DashboardCardDTO.builder()
                .id(id)
                .title(title)
                .value(value)
                .subtitle(subtitle)
                .icon(icon)
                .color(color)
                .trend(trend)
                .build();
    }

    private DashboardMetricDTO metric(String label, String value, String subtitle) {
        return DashboardMetricDTO.builder()
                .label(label)
                .value(value)
                .subtitle(subtitle)
                .build();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "₹0";
        }
        return "₹" + amount.stripTrailingZeros().toPlainString();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
