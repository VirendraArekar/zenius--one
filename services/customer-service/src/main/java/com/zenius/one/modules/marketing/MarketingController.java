package com.zenius.one.modules.marketing;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.marketing.MarketingService.CampaignDto;
import com.zenius.one.modules.marketing.MarketingService.CouponDto;
import com.zenius.one.modules.marketing.MarketingService.CreateCampaignRequest;
import com.zenius.one.modules.marketing.MarketingService.CreateCouponRequest;
import com.zenius.one.modules.marketing.MarketingService.CreateFormRequest;
import com.zenius.one.modules.marketing.MarketingService.CreateJourneyRequest;
import com.zenius.one.modules.marketing.MarketingService.CreateSegmentRequest;
import com.zenius.one.modules.marketing.MarketingService.FormDto;
import com.zenius.one.modules.marketing.MarketingService.FormSubmissionRequest;
import com.zenius.one.modules.marketing.MarketingService.FormSubmissionResult;
import com.zenius.one.modules.marketing.MarketingService.JourneyDto;
import com.zenius.one.modules.marketing.MarketingService.MarketingDashboardDto;
import com.zenius.one.modules.marketing.MarketingService.SegmentDto;
import com.zenius.one.modules.marketing.MarketingService.SubscriberDto;
import com.zenius.one.modules.marketing.MarketingService.UpsertSubscriberRequest;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/marketing")
@Validated
@Tag(name = "Marketing Automation", description = "Campaigns, subscribers, segments, journeys, forms, and coupons")
public class MarketingController {

    private final MarketingService marketingService;

    public MarketingController(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@authz.hasPermission('marketing:read')")
    public ApiResponse<MarketingDashboardDto> dashboard() {
        return ApiResponse.ok(marketingService.getDashboard());
    }

    @GetMapping("/subscribers")
    @PreAuthorize("@authz.hasPermission('marketing:read')")
    public ApiResponse<List<SubscriberDto>> listSubscribers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        Page<SubscriberDto> res = marketingService.listSubscribers(
                status, q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/subscribers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('marketing:write')")
    public ApiResponse<SubscriberDto> upsertSubscriber(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody SubscriberBody body
    ) {
        return ApiResponse.ok(marketingService.upsertSubscriber(
                principal,
                new UpsertSubscriberRequest(body.email(), body.fullName(), body.source(), body.tags(), body.status())
        ));
    }

    @GetMapping("/segments")
    @PreAuthorize("@authz.hasPermission('marketing:read')")
    public ApiResponse<List<SegmentDto>> listSegments() {
        return ApiResponse.ok(marketingService.listSegments());
    }

    @PostMapping("/segments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('marketing:write')")
    public ApiResponse<SegmentDto> createSegment(@Valid @RequestBody SegmentBody body) {
        return ApiResponse.ok(marketingService.createSegment(
                new CreateSegmentRequest(body.name(), body.description(), body.filterTag(), body.filterSource())
        ));
    }

    @GetMapping("/campaigns")
    @PreAuthorize("@authz.hasPermission('marketing:read')")
    public ApiResponse<List<CampaignDto>> listCampaigns(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        Page<CampaignDto> res = marketingService.listCampaigns(PageRequest.of(page, size));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('marketing:write')")
    public ApiResponse<CampaignDto> createCampaign(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody CampaignBody body
    ) {
        return ApiResponse.ok(marketingService.createCampaign(
                principal,
                new CreateCampaignRequest(body.segmentId(), body.name(), body.channel(), body.subject(), body.bodyContent(), body.scheduledAt())
        ));
    }

    @PostMapping("/campaigns/{id}/launch")
    @PreAuthorize("@authz.hasPermission('marketing:write')")
    public ApiResponse<CampaignDto> launchCampaign(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(marketingService.launchCampaign(principal, id));
    }

    @GetMapping("/journeys")
    @PreAuthorize("@authz.hasPermission('marketing:read')")
    public ApiResponse<List<JourneyDto>> listJourneys() {
        return ApiResponse.ok(marketingService.listJourneys());
    }

    @PostMapping("/journeys")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('marketing:write')")
    public ApiResponse<JourneyDto> createJourney(@Valid @RequestBody JourneyBody body) {
        return ApiResponse.ok(marketingService.createJourney(
                new CreateJourneyRequest(body.name(), body.triggerEvent(), body.stepsSummary())
        ));
    }

    @GetMapping("/forms")
    @PreAuthorize("@authz.hasPermission('marketing:read')")
    public ApiResponse<List<FormDto>> listForms() {
        return ApiResponse.ok(marketingService.listForms());
    }

    @PostMapping("/forms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('marketing:write')")
    public ApiResponse<FormDto> createForm(@Valid @RequestBody FormBody body) {
        return ApiResponse.ok(marketingService.createForm(
                new CreateFormRequest(body.name(), body.title(), body.submitLabel(), body.autoCreateLead())
        ));
    }

    @PostMapping("/forms/{id}/submit")
    @PreAuthorize("@authz.hasPermission('marketing:write')")
    public ApiResponse<FormSubmissionResult> submitForm(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody FormSubmissionBody body
    ) {
        return ApiResponse.ok(marketingService.submitForm(
                principal,
                id,
                new FormSubmissionRequest(body.fullName(), body.email(), body.phone(), body.companyName())
        ));
    }

    @GetMapping("/coupons")
    @PreAuthorize("@authz.hasPermission('marketing:read')")
    public ApiResponse<List<CouponDto>> listCoupons() {
        return ApiResponse.ok(marketingService.listCoupons());
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('marketing:write')")
    public ApiResponse<CouponDto> createCoupon(@Valid @RequestBody CouponBody body) {
        return ApiResponse.ok(marketingService.createCoupon(
                new CreateCouponRequest(body.code(), body.discountPercent(), body.maxRedemptions(), body.validUntil())
        ));
    }

    public record SubscriberBody(@NotBlank @Email String email, String fullName, String source, String tags, String status) {
    }

    public record SegmentBody(@NotBlank String name, String description, String filterTag, String filterSource) {
    }

    public record CampaignBody(UUID segmentId, @NotBlank String name, String channel, @NotBlank String subject, @NotBlank String bodyContent, Instant scheduledAt) {
    }

    public record JourneyBody(@NotBlank String name, @NotBlank String triggerEvent, @NotBlank String stepsSummary) {
    }

    public record FormBody(@NotBlank String name, @NotBlank String title, String submitLabel, boolean autoCreateLead) {
    }

    public record FormSubmissionBody(@NotBlank String fullName, @NotBlank @Email String email, String phone, String companyName) {
    }

    public record CouponBody(
            @NotBlank String code,
            @NotNull @DecimalMin("1.00") @DecimalMax("100.00") BigDecimal discountPercent,
            Integer maxRedemptions,
            LocalDate validUntil
    ) {
    }
}

