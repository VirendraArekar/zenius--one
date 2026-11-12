package com.zenius.one.modules.marketing;

import com.zenius.one.modules.crm.CrmLeadService;
import com.zenius.one.modules.crm.CrmLeadService.LeadMutationRequest;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingService {

    private final MktSubscriberRepository subscriberRepository;
    private final MktSegmentRepository segmentRepository;
    private final MktCampaignRepository campaignRepository;
    private final MktJourneyRepository journeyRepository;
    private final MktFormRepository formRepository;
    private final MktCouponRepository couponRepository;
    private final CrmLeadService crmLeadService;
    private final AuditLogService auditLogService;

    public MarketingService(
            MktSubscriberRepository subscriberRepository,
            MktSegmentRepository segmentRepository,
            MktCampaignRepository campaignRepository,
            MktJourneyRepository journeyRepository,
            MktFormRepository formRepository,
            MktCouponRepository couponRepository,
            CrmLeadService crmLeadService,
            AuditLogService auditLogService
    ) {
        this.subscriberRepository = subscriberRepository;
        this.segmentRepository = segmentRepository;
        this.campaignRepository = campaignRepository;
        this.journeyRepository = journeyRepository;
        this.formRepository = formRepository;
        this.couponRepository = couponRepository;
        this.crmLeadService = crmLeadService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public MarketingDashboardDto getDashboard() {
        UUID orgId = TenantContext.requireOrganizationId();
        long subscribedCount = subscriberRepository.countByOrganizationIdAndStatus(orgId, "SUBSCRIBED");
        List<MktCampaignEntity> campaigns = campaignRepository.findByOrganizationId(orgId);
        long sentCampaigns = campaigns.stream().filter(c -> "SENT".equals(c.getStatus())).count();
        long totalDispatched = campaigns.stream().mapToLong(MktCampaignEntity::getSentCount).sum();
        long totalOpens = campaigns.stream().mapToLong(MktCampaignEntity::getOpenedCount).sum();
        long totalClicks = campaigns.stream().mapToLong(MktCampaignEntity::getClickedCount).sum();
        long totalFormSubmissions = formRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .mapToLong(MktFormEntity::getSubmissionsCount).sum();
        long activeCoupons = couponRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .filter(MktCouponEntity::isActive).count();

        return new MarketingDashboardDto(
                subscribedCount,
                campaigns.size(),
                sentCampaigns,
                totalDispatched,
                totalOpens,
                totalClicks,
                totalFormSubmissions,
                activeCoupons
        );
    }

    @Transactional(readOnly = true)
    public Page<SubscriberDto> listSubscribers(String status, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return subscriberRepository.search(orgId, norm(status), norm(query), pageable).map(this::toSubscriberDto);
    }

    @Transactional
    public SubscriberDto upsertSubscriber(ZeniusUserPrincipal principal, UpsertSubscriberRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String email = req.email().trim().toLowerCase();
        MktSubscriberEntity entity = subscriberRepository.findByOrganizationIdAndEmailIgnoreCase(orgId, email)
                .orElseGet(MktSubscriberEntity::new);
        if (entity.getOrganizationId() == null) {
            entity.setOrganizationId(orgId);
        }
        entity.setEmail(email);
        entity.setFullName(norm(req.fullName()));
        entity.setSource(req.source() != null && !req.source().isBlank() ? req.source().trim().toUpperCase() : "MANUAL");
        entity.setTags(norm(req.tags()));
        entity.setStatus(req.status() != null && !req.status().isBlank() ? req.status().trim().toUpperCase() : "SUBSCRIBED");
        entity = subscriberRepository.save(entity);
        return toSubscriberDto(entity);
    }

    @Transactional(readOnly = true)
    public List<SegmentDto> listSegments() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<MktSubscriberEntity> subscribed = subscriberRepository.findByOrganizationIdAndStatus(orgId, "SUBSCRIBED");
        return segmentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(s -> {
                    long count = countMatchingSubscribers(subscribed, s);
                    return new SegmentDto(s.getId(), s.getName(), s.getDescription(), s.getFilterTag(), s.getFilterSource(), count);
                })
                .toList();
    }

    @Transactional
    public SegmentDto createSegment(CreateSegmentRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        MktSegmentEntity seg = new MktSegmentEntity();
        seg.setOrganizationId(orgId);
        seg.setName(req.name().trim());
        seg.setDescription(norm(req.description()));
        seg.setFilterTag(norm(req.filterTag()));
        seg.setFilterSource(norm(req.filterSource()));
        seg = segmentRepository.save(seg);
        List<MktSubscriberEntity> subscribed = subscriberRepository.findByOrganizationIdAndStatus(orgId, "SUBSCRIBED");
        return new SegmentDto(seg.getId(), seg.getName(), seg.getDescription(), seg.getFilterTag(), seg.getFilterSource(),
                countMatchingSubscribers(subscribed, seg));
    }

    @Transactional(readOnly = true)
    public Page<CampaignDto> listCampaigns(Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return campaignRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable).map(this::toCampaignDto);
    }

    @Transactional
    public CampaignDto createCampaign(ZeniusUserPrincipal principal, CreateCampaignRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (req.segmentId() != null && segmentRepository.findByIdAndOrganizationId(req.segmentId(), orgId).isEmpty()) {
            throw new IllegalArgumentException("Segment does not belong to current organization");
        }
        MktCampaignEntity c = new MktCampaignEntity();
        c.setOrganizationId(orgId);
        c.setSegmentId(req.segmentId());
        c.setName(req.name().trim());
        c.setChannel(req.channel() != null && !req.channel().isBlank() ? req.channel().trim().toUpperCase() : "EMAIL");
        c.setSubject(req.subject().trim());
        c.setBodyContent(req.bodyContent().trim());
        c.setStatus("DRAFT");
        c.setScheduledAt(req.scheduledAt());
        c = campaignRepository.save(c);

        auditLogService.record(orgId, principal.getUserId(), principal.getEmail(),
                "MKT_CAMPAIGN_CREATED", "MKT_CAMPAIGN", c.getId().toString(),
                "Created marketing campaign " + c.getName(), "SUCCESS");
        return toCampaignDto(c);
    }

    @Transactional
    public CampaignDto launchCampaign(ZeniusUserPrincipal principal, UUID campaignId) {
        UUID orgId = TenantContext.requireOrganizationId();
        MktCampaignEntity c = campaignRepository.findByIdAndOrganizationId(campaignId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        if ("SENT".equals(c.getStatus())) {
            throw new IllegalStateException("Campaign has already been launched");
        }

        List<MktSubscriberEntity> subscribed = subscriberRepository.findByOrganizationIdAndStatus(orgId, "SUBSCRIBED");
        int audienceSize;
        if (c.getSegmentId() != null) {
            MktSegmentEntity seg = segmentRepository.findByIdAndOrganizationId(c.getSegmentId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found"));
            audienceSize = (int) countMatchingSubscribers(subscribed, seg);
        } else {
            audienceSize = subscribed.size();
        }

        c.setStatus("SENT");
        c.setSentAt(Instant.now());
        c.setSentCount(audienceSize);
        c = campaignRepository.save(c);

        auditLogService.record(orgId, principal.getUserId(), principal.getEmail(),
                "MKT_CAMPAIGN_LAUNCHED", "MKT_CAMPAIGN", c.getId().toString(),
                "Launched campaign " + c.getName() + " to " + audienceSize + " subscribers", "SUCCESS");
        return toCampaignDto(c);
    }

    @Transactional(readOnly = true)
    public List<JourneyDto> listJourneys() {
        UUID orgId = TenantContext.requireOrganizationId();
        return journeyRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(j -> new JourneyDto(j.getId(), j.getName(), j.getTriggerEvent(), j.getStepsSummary(), j.getStatus(), j.getEnrolledCount()))
                .toList();
    }

    @Transactional
    public JourneyDto createJourney(CreateJourneyRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        MktJourneyEntity j = new MktJourneyEntity();
        j.setOrganizationId(orgId);
        j.setName(req.name().trim());
        j.setTriggerEvent(req.triggerEvent().trim().toUpperCase());
        j.setStepsSummary(req.stepsSummary().trim());
        j.setStatus("ACTIVE");
        j = journeyRepository.save(j);
        return new JourneyDto(j.getId(), j.getName(), j.getTriggerEvent(), j.getStepsSummary(), j.getStatus(), j.getEnrolledCount());
    }

    @Transactional(readOnly = true)
    public List<FormDto> listForms() {
        UUID orgId = TenantContext.requireOrganizationId();
        return formRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(f -> new FormDto(f.getId(), f.getName(), f.getTitle(), f.getSubmitLabel(), f.isAutoCreateLead(), f.getSubmissionsCount(), f.getStatus()))
                .toList();
    }

    @Transactional
    public FormDto createForm(CreateFormRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        MktFormEntity f = new MktFormEntity();
        f.setOrganizationId(orgId);
        f.setName(req.name().trim());
        f.setTitle(req.title().trim());
        f.setSubmitLabel(req.submitLabel() != null && !req.submitLabel().isBlank() ? req.submitLabel().trim() : "Submit");
        f.setAutoCreateLead(req.autoCreateLead());
        f.setStatus("ACTIVE");
        f = formRepository.save(f);
        return new FormDto(f.getId(), f.getName(), f.getTitle(), f.getSubmitLabel(), f.isAutoCreateLead(), f.getSubmissionsCount(), f.getStatus());
    }

    @Transactional
    public FormSubmissionResult submitForm(ZeniusUserPrincipal principal, UUID formId, FormSubmissionRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        MktFormEntity form = formRepository.findByIdAndOrganizationId(formId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + formId));

        SubscriberDto sub = upsertSubscriber(principal, new UpsertSubscriberRequest(
                req.email(), req.fullName(), "WEB_FORM", "form:" + form.getName(), "SUBSCRIBED"
        ));

        UUID createdLeadId = null;
        if (form.isAutoCreateLead()) {
            try {
                var lead = crmLeadService.create(principal, new LeadMutationRequest(
                        req.fullName(), req.email(), req.phone(), req.companyName(), null,
                        "WEB_FORM", "NEW", null, "Captured from marketing form: " + form.getName(), null
                ));
                createdLeadId = lead.id();
            } catch (IllegalStateException ignored) {
                // Duplicate lead email in CRM; keep form submission successful
            }
        }

        form.setSubmissionsCount(form.getSubmissionsCount() + 1);
        formRepository.save(form);
        return new FormSubmissionResult(form.getId(), sub.id(), createdLeadId, form.getSubmissionsCount());
    }

    @Transactional(readOnly = true)
    public List<CouponDto> listCoupons() {
        UUID orgId = TenantContext.requireOrganizationId();
        return couponRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(c -> new CouponDto(c.getId(), c.getCode(), c.getDiscountPercent(), c.getMaxRedemptions(), c.getRedemptionCount(), c.getValidUntil(), c.isActive()))
                .toList();
    }

    @Transactional
    public CouponDto createCoupon(CreateCouponRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String code = req.code().trim().toUpperCase();
        if (couponRepository.existsByOrganizationIdAndCodeIgnoreCase(orgId, code)) {
            throw new IllegalStateException("Coupon code already exists: " + code);
        }
        MktCouponEntity c = new MktCouponEntity();
        c.setOrganizationId(orgId);
        c.setCode(code);
        c.setDiscountPercent(req.discountPercent());
        c.setMaxRedemptions(req.maxRedemptions());
        c.setValidUntil(req.validUntil());
        c.setActive(true);
        c = couponRepository.save(c);
        return new CouponDto(c.getId(), c.getCode(), c.getDiscountPercent(), c.getMaxRedemptions(), c.getRedemptionCount(), c.getValidUntil(), c.isActive());
    }

    private long countMatchingSubscribers(List<MktSubscriberEntity> subscribed, MktSegmentEntity seg) {
        return subscribed.stream().filter(sub -> {
            if (seg.getFilterTag() != null && !seg.getFilterTag().isBlank()) {
                if (sub.getTags() == null || !sub.getTags().toLowerCase().contains(seg.getFilterTag().toLowerCase())) {
                    return false;
                }
            }
            if (seg.getFilterSource() != null && !seg.getFilterSource().isBlank()) {
                return seg.getFilterSource().equalsIgnoreCase(sub.getSource());
            }
            return true;
        }).count();
    }

    private SubscriberDto toSubscriberDto(MktSubscriberEntity s) {
        return new SubscriberDto(s.getId(), s.getEmail(), s.getFullName(), s.getSource(), s.getTags(), s.getStatus(), s.getCreatedAt());
    }

    private CampaignDto toCampaignDto(MktCampaignEntity c) {
        return new CampaignDto(c.getId(), c.getSegmentId(), c.getName(), c.getChannel(), c.getSubject(), c.getStatus(), c.getScheduledAt(), c.getSentAt(), c.getSentCount(), c.getOpenedCount(), c.getClickedCount(), c.getCreatedAt());
    }

    private String norm(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    public record MarketingDashboardDto(long subscribedCount, long totalCampaigns, long sentCampaigns, long totalDispatched, long totalOpens, long totalClicks, long totalFormSubmissions, long activeCoupons) {
    }

    public record UpsertSubscriberRequest(String email, String fullName, String source, String tags, String status) {
    }

    public record SubscriberDto(UUID id, String email, String fullName, String source, String tags, String status, Instant createdAt) {
    }

    public record CreateSegmentRequest(String name, String description, String filterTag, String filterSource) {
    }

    public record SegmentDto(UUID id, String name, String description, String filterTag, String filterSource, long matchingSubscribers) {
    }

    public record CreateCampaignRequest(UUID segmentId, String name, String channel, String subject, String bodyContent, Instant scheduledAt) {
    }

    public record CampaignDto(UUID id, UUID segmentId, String name, String channel, String subject, String status, Instant scheduledAt, Instant sentAt, Integer sentCount, Integer openedCount, Integer clickedCount, Instant createdAt) {
    }

    public record CreateJourneyRequest(String name, String triggerEvent, String stepsSummary) {
    }

    public record JourneyDto(UUID id, String name, String triggerEvent, String stepsSummary, String status, Integer enrolledCount) {
    }

    public record CreateFormRequest(String name, String title, String submitLabel, boolean autoCreateLead) {
    }

    public record FormDto(UUID id, String name, String title, String submitLabel, boolean autoCreateLead, Integer submissionsCount, String status) {
    }

    public record FormSubmissionRequest(String fullName, String email, String phone, String companyName) {
    }

    public record FormSubmissionResult(UUID formId, UUID subscriberId, UUID createdLeadId, int totalSubmissions) {
    }

    public record CreateCouponRequest(String code, BigDecimal discountPercent, Integer maxRedemptions, LocalDate validUntil) {
    }

    public record CouponDto(UUID id, String code, BigDecimal discountPercent, Integer maxRedemptions, Integer redemptionCount, LocalDate validUntil, boolean active) {
    }
}

