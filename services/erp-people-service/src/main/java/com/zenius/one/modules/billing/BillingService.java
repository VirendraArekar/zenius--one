package com.zenius.one.modules.billing;

import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private final BillPlanRepository planRepository;
    private final BillSubscriptionRepository subscriptionRepository;

    public BillingService(BillPlanRepository planRepository, BillSubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanDto> listPlans() {
        UUID orgId = TenantContext.requireOrganizationId();
        return planRepository.findByOrganizationIdOrderByPriceAsc(orgId).stream()
                .map(p -> new PlanDto(p.getId(), p.getCode(), p.getName(), p.getBillingInterval(), p.getPrice(), p.getCurrency(), p.getTrialDays(), p.isActive()))
                .toList();
    }

    @Transactional
    public PlanDto createPlan(CreatePlanRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String code = req.code().trim().toUpperCase();
        if (planRepository.existsByOrganizationIdAndCodeIgnoreCase(orgId, code)) {
            throw new IllegalStateException("Subscription plan code already exists: " + code);
        }
        BillPlanEntity p = new BillPlanEntity();
        p.setOrganizationId(orgId);
        p.setCode(code);
        p.setName(req.name().trim());
        p.setBillingInterval(req.billingInterval() != null ? req.billingInterval().trim().toUpperCase() : "MONTHLY");
        p.setPrice(req.price());
        p.setCurrency(req.currency() != null ? req.currency().trim().toUpperCase() : "USD");
        p.setTrialDays(req.trialDays() != null ? req.trialDays() : 0);
        p.setActive(true);
        p = planRepository.save(p);
        return new PlanDto(p.getId(), p.getCode(), p.getName(), p.getBillingInterval(), p.getPrice(), p.getCurrency(), p.getTrialDays(), p.isActive());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDto> listSubscriptions() {
        UUID orgId = TenantContext.requireOrganizationId();
        Map<UUID, BillPlanEntity> plansById = planRepository.findByOrganizationIdOrderByPriceAsc(orgId).stream()
                .collect(Collectors.toMap(BillPlanEntity::getId, Function.identity()));
        return subscriptionRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(s -> toDto(s, plansById.get(s.getPlanId())))
                .toList();
    }

    @Transactional
    public SubscriptionDto subscribe(CreateSubscriptionRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        BillPlanEntity plan = planRepository.findByIdAndOrganizationId(req.planId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found in current organization"));

        LocalDate today = LocalDate.now();
        boolean hasTrial = plan.getTrialDays() > 0;
        LocalDate end = hasTrial
                ? today.plusDays(plan.getTrialDays())
                : ("YEARLY".equals(plan.getBillingInterval()) ? today.plusYears(1) : today.plusMonths(1));

        long seq = subscriptionRepository.countByOrganizationId(orgId) + 1;
        BillSubscriptionEntity sub = new BillSubscriptionEntity();
        sub.setOrganizationId(orgId);
        sub.setSubscriptionNumber(String.format("SUB-%05d", seq));
        sub.setPlanId(plan.getId());
        sub.setCustomerName(req.customerName().trim());
        sub.setCustomerEmail(req.customerEmail().trim().toLowerCase());
        sub.setStatus(hasTrial ? "TRIALING" : "ACTIVE");
        sub.setCurrentPeriodStart(today);
        sub.setCurrentPeriodEnd(end);
        sub = subscriptionRepository.save(sub);
        return toDto(sub, plan);
    }

    @Transactional
    public SubscriptionDto changePlan(UUID subscriptionId, UUID newPlanId) {
        UUID orgId = TenantContext.requireOrganizationId();
        BillSubscriptionEntity sub = subscriptionRepository.findByIdAndOrganizationId(subscriptionId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));
        if ("CANCELLED".equals(sub.getStatus())) {
            throw new IllegalStateException("Cannot change plan of cancelled subscription");
        }
        BillPlanEntity oldPlan = planRepository.findByIdAndOrganizationId(sub.getPlanId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Current plan not found"));
        BillPlanEntity newPlan = planRepository.findByIdAndOrganizationId(newPlanId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Target plan not found in current organization"));

        long totalDays = Math.max(1, ChronoUnit.DAYS.between(sub.getCurrentPeriodStart(), sub.getCurrentPeriodEnd()));
        long remainingDays = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), sub.getCurrentPeriodEnd()));
        BigDecimal unusedFraction = BigDecimal.valueOf(remainingDays).divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
        BigDecimal credit = oldPlan.getPrice().multiply(unusedFraction).setScale(2, RoundingMode.HALF_UP);

        sub.setPlanId(newPlan.getId());
        sub.setStatus("ACTIVE");
        sub.setCreditBalance(sub.getCreditBalance().add(credit));
        sub = subscriptionRepository.save(sub);
        return toDto(sub, newPlan);
    }

    @Transactional
    public SubscriptionDto cancel(UUID subscriptionId) {
        UUID orgId = TenantContext.requireOrganizationId();
        BillSubscriptionEntity sub = subscriptionRepository.findByIdAndOrganizationId(subscriptionId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));
        sub.setStatus("CANCELLED");
        sub = subscriptionRepository.save(sub);
        BillPlanEntity plan = planRepository.findByIdAndOrganizationId(sub.getPlanId(), orgId).orElse(null);
        return toDto(sub, plan);
    }

    private SubscriptionDto toDto(BillSubscriptionEntity s, BillPlanEntity p) {
        return new SubscriptionDto(
                s.getId(),
                s.getSubscriptionNumber(),
                s.getPlanId(),
                p != null ? p.getName() : "",
                p != null ? p.getPrice() : BigDecimal.ZERO,
                s.getCustomerName(),
                s.getCustomerEmail(),
                s.getStatus(),
                s.getCurrentPeriodStart(),
                s.getCurrentPeriodEnd(),
                s.getCreditBalance(),
                s.getDunningAttempts()
        );
    }

    public record PlanDto(UUID id, String code, String name, String billingInterval, BigDecimal price, String currency, Integer trialDays, boolean active) {
    }

    public record CreatePlanRequest(String code, String name, String billingInterval, BigDecimal price, String currency, Integer trialDays) {
    }

    public record CreateSubscriptionRequest(UUID planId, String customerName, String customerEmail) {
    }

    public record SubscriptionDto(UUID id, String subscriptionNumber, UUID planId, String planName, BigDecimal planPrice, String customerName, String customerEmail, String status, LocalDate currentPeriodStart, LocalDate currentPeriodEnd, BigDecimal creditBalance, Integer dunningAttempts) {
    }
}

