package com.zenius.one.modules.crm;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmDealService {

    private final CrmDealRepository dealRepository;
    private final CrmPipelineRepository pipelineRepository;
    private final CrmStageRepository stageRepository;
    private final CrmAccountRepository accountRepository;
    private final CrmContactRepository contactRepository;
    private final CrmLeadRepository leadRepository;
    private final CrmPipelineService pipelineService;
    private final AuditLogService auditLogService;

    public CrmDealService(
            CrmDealRepository dealRepository,
            CrmPipelineRepository pipelineRepository,
            CrmStageRepository stageRepository,
            CrmAccountRepository accountRepository,
            CrmContactRepository contactRepository,
            CrmLeadRepository leadRepository,
            CrmPipelineService pipelineService,
            AuditLogService auditLogService
    ) {
        this.dealRepository = dealRepository;
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.leadRepository = leadRepository;
        this.pipelineService = pipelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<DealDto> listDeals(UUID pipelineId, UUID stageId, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        String normQuery = (query == null || query.isBlank()) ? null : query.trim();
        Page<CrmDealEntity> page = dealRepository.search(orgId, pipelineId, stageId, normQuery, pageable);
        Map<UUID, CrmStageEntity> stagesById = stageRepository.findAllById(
                page.getContent().stream().map(CrmDealEntity::getStageId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(CrmStageEntity::getId, Function.identity()));

        return page.map(d -> toDto(d, stagesById.get(d.getStageId())));
    }

    @Transactional
    public DealDto createDeal(ZeniusUserPrincipal principal, CreateDealRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        var defaultPipeline = pipelineService.getOrProvisionDefaultPipeline();
        UUID pipelineId = req.pipelineId() != null ? req.pipelineId() : defaultPipeline.id();

        pipelineRepository.findByIdAndOrganizationId(pipelineId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found in current organization"));

        List<CrmStageEntity> stages = stageRepository.findByOrganizationIdAndPipelineIdOrderByStageOrderAsc(orgId, pipelineId);
        if (stages.isEmpty()) {
            throw new IllegalStateException("Pipeline has no configured stages");
        }
        CrmStageEntity stage = req.stageId() != null
                ? stages.stream()
                        .filter(s -> s.getId().equals(req.stageId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Stage does not belong to selected pipeline"))
                : stages.getFirst();

        validateRefs(orgId, req.accountId(), req.contactId());

        BigDecimal amount = req.amount() != null ? req.amount() : BigDecimal.ZERO;
        int probability = stage.getProbability();

        CrmDealEntity deal = new CrmDealEntity();
        deal.setOrganizationId(orgId);
        deal.setPipelineId(pipelineId);
        deal.setStageId(stage.getId());
        deal.setAccountId(req.accountId());
        deal.setContactId(req.contactId());
        deal.setOwnerUserId(principal.getUserId());
        deal.setTitle(req.title().trim());
        deal.setAmount(amount);
        deal.setProbability(probability);
        deal.setExpectedRevenue(computeExpected(amount, probability));
        deal.setExpectedCloseDate(req.expectedCloseDate());
        deal = dealRepository.save(deal);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_DEAL_CREATED",
                "CRM_DEAL",
                deal.getId().toString(),
                "Created deal " + deal.getTitle() + " ($" + deal.getAmount() + ")",
                "SUCCESS"
        );
        return toDto(deal, stage);
    }

    @Transactional
    public DealDto moveStage(ZeniusUserPrincipal principal, UUID dealId, MoveDealStageRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmDealEntity deal = dealRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(dealId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found: " + dealId));

        UUID dealPipelineId = deal.getPipelineId();
        CrmStageEntity targetStage = stageRepository.findByIdAndOrganizationId(req.stageId(), orgId)
                .filter(s -> s.getPipelineId().equals(dealPipelineId))
                .orElseThrow(() -> new IllegalArgumentException("Target stage not found in deal pipeline"));

        if (targetStage.isClosed() && !targetStage.isWon()) {
            if (req.lostReason() == null || req.lostReason().isBlank()) {
                throw new IllegalArgumentException("Lost reason is required when moving deal to Closed Lost");
            }
            deal.setLostReason(req.lostReason().trim());
        } else {
            deal.setLostReason(null);
        }

        deal.setStageId(targetStage.getId());
        deal.setProbability(targetStage.getProbability());
        deal.setExpectedRevenue(computeExpected(deal.getAmount(), targetStage.getProbability()));
        deal = dealRepository.save(deal);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_DEAL_STAGE_MOVED",
                "CRM_DEAL",
                deal.getId().toString(),
                "Moved deal " + deal.getTitle() + " to stage " + targetStage.getName(),
                "SUCCESS"
        );
        return toDto(deal, targetStage);
    }

    @Transactional
    public CrmDashboardDto getDashboardSummary() {
        UUID orgId = TenantContext.requireOrganizationId();
        var defaultPipeline = pipelineService.getOrProvisionDefaultPipeline();
        List<CrmDealEntity> deals = dealRepository.findByOrganizationIdAndDeletedAtIsNull(orgId);
        List<CrmLeadEntity> leads = leadRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(orgId);

        Map<UUID, CrmStageEntity> stageMap = stageRepository
                .findByOrganizationIdAndPipelineIdOrderByStageOrderAsc(orgId, defaultPipeline.id())
                .stream()
                .collect(Collectors.toMap(CrmStageEntity::getId, Function.identity()));

        BigDecimal openPipelineAmount = BigDecimal.ZERO;
        BigDecimal weightedForecast = BigDecimal.ZERO;
        BigDecimal closedWonAmount = BigDecimal.ZERO;
        long openDeals = 0;

        Map<UUID, StageSummaryAccumulator> byStage = new LinkedHashMap<>();
        for (var s : defaultPipeline.stages()) {
            byStage.put(s.id(), new StageSummaryAccumulator(s.id(), s.name()));
        }

        for (CrmDealEntity d : deals) {
            CrmStageEntity stage = stageMap.get(d.getStageId());
            if (stage != null) {
                var acc = byStage.get(stage.getId());
                if (acc != null) {
                    acc.count++;
                    acc.amount = acc.amount.add(d.getAmount());
                }
                if (stage.isClosed()) {
                    if (stage.isWon()) {
                        closedWonAmount = closedWonAmount.add(d.getAmount());
                    }
                } else {
                    openDeals++;
                    openPipelineAmount = openPipelineAmount.add(d.getAmount());
                    weightedForecast = weightedForecast.add(d.getExpectedRevenue());
                }
            }
        }

        Map<String, Long> sourceCounts = leads.stream()
                .collect(Collectors.groupingBy(CrmLeadEntity::getSource, Collectors.counting()));
        List<SourceBreakdownDto> leadsBySource = sourceCounts.entrySet().stream()
                .map(e -> new SourceBreakdownDto(e.getKey(), e.getValue()))
                .toList();

        long totalLeads = leads.size();
        long convertedLeads = leads.stream().filter(l -> "CONVERTED".equals(l.getStatus())).count();
        long totalAccounts = accountRepository.countByOrganizationIdAndDeletedAtIsNull(orgId);
        long totalContacts = contactRepository.countByOrganizationIdAndDeletedAtIsNull(orgId);

        List<StageBreakdownDto> stageBreakdown = new ArrayList<>();
        for (var acc : byStage.values()) {
            stageBreakdown.add(new StageBreakdownDto(acc.stageId, acc.stageName, acc.count, acc.amount));
        }

        return new CrmDashboardDto(
                totalLeads,
                convertedLeads,
                totalAccounts,
                totalContacts,
                openDeals,
                openPipelineAmount,
                weightedForecast,
                closedWonAmount,
                stageBreakdown,
                leadsBySource
        );
    }

    private void validateRefs(UUID orgId, UUID accountId, UUID contactId) {
        if (accountId != null
                && accountRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(accountId, orgId).isEmpty()) {
            throw new IllegalArgumentException("Account does not belong to current organization");
        }
        if (contactId != null
                && contactRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(contactId, orgId).isEmpty()) {
            throw new IllegalArgumentException("Contact does not belong to current organization");
        }
    }

    private BigDecimal computeExpected(BigDecimal amount, int probability) {
        return amount.multiply(BigDecimal.valueOf(probability))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private DealDto toDto(CrmDealEntity d, CrmStageEntity s) {
        return new DealDto(
                d.getId(),
                d.getPipelineId(),
                d.getStageId(),
                s != null ? s.getName() : "",
                s != null && s.isClosed(),
                s != null && s.isWon(),
                d.getAccountId(),
                d.getContactId(),
                d.getOwnerUserId(),
                d.getTitle(),
                d.getAmount(),
                d.getProbability(),
                d.getExpectedRevenue(),
                d.getExpectedCloseDate(),
                d.getLostReason(),
                d.getCreatedAt()
        );
    }

    private static final class StageSummaryAccumulator {
        final UUID stageId;
        final String stageName;
        long count = 0;
        BigDecimal amount = BigDecimal.ZERO;

        StageSummaryAccumulator(UUID stageId, String stageName) {
            this.stageId = stageId;
            this.stageName = stageName;
        }
    }

    public record CreateDealRequest(
            UUID pipelineId,
            UUID stageId,
            UUID accountId,
            UUID contactId,
            String title,
            BigDecimal amount,
            LocalDate expectedCloseDate
    ) {
    }

    public record MoveDealStageRequest(UUID stageId, String lostReason) {
    }

    public record DealDto(
            UUID id,
            UUID pipelineId,
            UUID stageId,
            String stageName,
            boolean closed,
            boolean won,
            UUID accountId,
            UUID contactId,
            UUID ownerUserId,
            String title,
            BigDecimal amount,
            Integer probability,
            BigDecimal expectedRevenue,
            LocalDate expectedCloseDate,
            String lostReason,
            Instant createdAt
    ) {
    }

    public record StageBreakdownDto(UUID stageId, String stageName, long dealCount, BigDecimal totalAmount) {
    }

    public record SourceBreakdownDto(String source, long leadCount) {
    }

    public record CrmDashboardDto(
            long totalLeads,
            long convertedLeads,
            long totalAccounts,
            long totalContacts,
            long openDeals,
            BigDecimal openPipelineAmount,
            BigDecimal weightedForecast,
            BigDecimal closedWonAmount,
            List<StageBreakdownDto> stageBreakdown,
            List<SourceBreakdownDto> leadsBySource
    ) {
    }
}
