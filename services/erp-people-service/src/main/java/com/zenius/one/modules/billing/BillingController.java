package com.zenius.one.modules.billing;

import com.zenius.one.modules.billing.BillingService.CreatePlanRequest;
import com.zenius.one.modules.billing.BillingService.CreateSubscriptionRequest;
import com.zenius.one.modules.billing.BillingService.PlanDto;
import com.zenius.one.modules.billing.BillingService.SubscriptionDto;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing & Subscriptions", description = "Subscription plans, free trials, proration upgrades, and cancellations")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/plans")
    @PreAuthorize("@authz.hasPermission('billing:read')")
    public ApiResponse<List<PlanDto>> listPlans() {
        return ApiResponse.ok(billingService.listPlans());
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('billing:write')")
    public ApiResponse<PlanDto> createPlan(@Valid @RequestBody PlanBody body) {
        return ApiResponse.ok(billingService.createPlan(
                new CreatePlanRequest(body.code(), body.name(), body.billingInterval(), body.price(), body.currency(), body.trialDays())
        ));
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("@authz.hasPermission('billing:read')")
    public ApiResponse<List<SubscriptionDto>> listSubscriptions() {
        return ApiResponse.ok(billingService.listSubscriptions());
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('billing:write')")
    public ApiResponse<SubscriptionDto> subscribe(@Valid @RequestBody SubscribeBody body) {
        return ApiResponse.ok(billingService.subscribe(
                new CreateSubscriptionRequest(body.planId(), body.customerName(), body.customerEmail())
        ));
    }

    @PostMapping("/subscriptions/{id}/change-plan")
    @PreAuthorize("@authz.hasPermission('billing:write')")
    public ApiResponse<SubscriptionDto> changePlan(@PathVariable UUID id, @Valid @RequestBody ChangePlanBody body) {
        return ApiResponse.ok(billingService.changePlan(id, body.planId()));
    }

    @PostMapping("/subscriptions/{id}/cancel")
    @PreAuthorize("@authz.hasPermission('billing:write')")
    public ApiResponse<SubscriptionDto> cancel(@PathVariable UUID id) {
        return ApiResponse.ok(billingService.cancel(id));
    }

    public record PlanBody(@NotBlank String code, @NotBlank String name, String billingInterval, @NotNull @DecimalMin("0.00") BigDecimal price, String currency, Integer trialDays) {
    }

    public record SubscribeBody(@NotNull UUID planId, @NotBlank String customerName, @NotBlank @Email String customerEmail) {
    }

    public record ChangePlanBody(@NotNull UUID planId) {
    }
}

