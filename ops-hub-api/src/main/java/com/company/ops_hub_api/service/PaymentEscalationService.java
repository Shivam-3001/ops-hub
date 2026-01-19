package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Customer;
import com.company.ops_hub_api.domain.Payment;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.PaymentRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.util.HierarchyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEscalationService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *") // Daily at 08:00
    @Transactional
    public void evaluateEscalations() {
        escalatePaymentsOlderThan(7, HierarchyUtil.AREA_HEAD, "WARNING");
        escalatePaymentsOlderThan(15, HierarchyUtil.CIRCLE_HEAD, "CRITICAL");
    }

    private void escalatePaymentsOlderThan(int days, String targetRole, String severity) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Payment> overduePayments = paymentRepository.findByPaymentStatusAndCreatedAtBefore("INITIATED", cutoff);
        for (Payment payment : overduePayments) {
            Customer customer = payment.getCustomer();
            if (customer == null || customer.getArea() == null) {
                continue;
            }

            List<User> targets = resolveTargets(customer, targetRole);
            for (User target : targets) {
                notificationService.notifyUserOnce(
                        target,
                        "ESCALATION",
                        "Payment pending escalation",
                        String.format("Payment %s pending for %d+ days (Customer %s).",
                                payment.getPaymentReference(), days, customer.getCustomerCode()),
                        "PAYMENT",
                        payment.getId(),
                        severity
                );
            }
        }
    }

    private List<User> resolveTargets(Customer customer, String targetRole) {
        if (HierarchyUtil.AREA_HEAD.equals(targetRole)) {
            return userRepository.findByAreaIdAndUserType(customer.getArea().getId(), HierarchyUtil.AREA_HEAD);
        }
        if (HierarchyUtil.CIRCLE_HEAD.equals(targetRole)) {
            Long circleId = HierarchyUtil.getCircleId(customer.getArea());
            if (circleId == null) {
                return List.of();
            }
            return userRepository.findByAreaZoneCircleIdAndUserType(circleId, HierarchyUtil.CIRCLE_HEAD);
        }
        return List.of();
    }
}
