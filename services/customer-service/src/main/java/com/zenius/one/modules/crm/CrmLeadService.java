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
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmLeadService {

    private final CrmLeadRepository leadRepository;
    private final CrmAccountRepository accountRepository;
    private final CrmContactRepository contactRepository;
    private final CrmDealRepository dealRepository;
    private final CrmPipelineService pipelineService;
    private final CrmActivityRepository activityRepository;
    private final AuditLogService auditLogService;

    public CrmLeadService(
            CrmLeadRepository leadRepository,
            CrmAccountRepository accountRepository,
            CrmContactRepository contactRepository,
            CrmDealRepository dealRepository,
            CrmPipelineService pipelineService,
            CrmActivityRepository activityRepository,
            AuditLogService auditLogService
    ) {
        this.leadRepository = leadRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.dealRepository = dealRepository;
        this.pipelineService = pipelineService;
        this.activityRepository = activityRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<LeadDto> list(String status, String source, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return leadRepository.search(orgId, norm(status), norm(source), norm(query), pageable).map(this::toDto);
    }

    @Transactional
    public LeadDto create(ZeniusUserPrincipal principal, LeadMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String email = norm(req.email());
        if (email != null && leadRepository.existsByOrganizationIdAndEmailIgnoreCaseAndDeletedAtIsNull(orgId, email)) {
            throw new IllegalStateException("Duplicate lead detected with email: " + email);
        }

        CrmLeadEntity lead = new CrmLeadEntity();
        lead.setOrganizationId(orgId);
        lead.setOwnerUserId(req.ownerUserId() != null ? req.ownerUserId() : principal.getUserId());
        lead.setFullName(req.fullName().trim());
        lead.setEmail(email);
        lead.setPhone(norm(req.phone()));
        lead.setCompanyName(norm(req.companyName()));
        lead.setJobTitle(norm(req.jobTitle()));
        lead.setSource(req.source() != null && !req.source().isBlank() ? req.source().trim().toUpperCase() : "WEBSITE");
        lead.setStatus(req.status() != null && !req.status().isBlank() ? req.status().trim().toUpperCase() : "NEW");
        lead.setEstimatedValue(req.estimatedValue());
        lead.setNotes(norm(req.notes()));
        lead.setLeadScore(computeLeadScore(lead));
        lead = leadRepository.save(lead);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_LEAD_CREATED",
                "CRM_LEAD",
                lead.getId().toString(),
                "Created CRM lead " + lead.getFullName() + " (score " + lead.getLeadScore() + ")",
                "SUCCESS"
        );
        return toDto(lead);
    }

    @Transactional
    public LeadDto update(ZeniusUserPrincipal principal, UUID leadId, LeadMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmLeadEntity lead = leadRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(leadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));

        if ("CONVERTED".equals(lead.getStatus())) {
            throw new IllegalStateException("Converted leads cannot be modified");
        }

        lead.setFullName(req.fullName().trim());
        lead.setEmail(norm(req.email()));
        lead.setPhone(norm(req.phone()));
        lead.setCompanyName(norm(req.companyName()));
        lead.setJobTitle(norm(req.jobTitle()));
        if (req.source() != null && !req.source().isBlank()) {
            lead.setSource(req.source().trim().toUpperCase());
        }
        if (req.status() != null && !req.status().isBlank()) {
            lead.setStatus(req.status().trim().toUpperCase());
        }
        if (req.ownerUserId() != null) {
            lead.setOwnerUserId(req.ownerUserId());
        }
        lead.setEstimatedValue(req.estimatedValue());
        lead.setNotes(norm(req.notes()));
        lead.setLeadScore(computeLeadScore(lead));
        lead = leadRepository.save(lead);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_LEAD_UPDATED",
                "CRM_LEAD",
                lead.getId().toString(),
                "Updated CRM lead " + lead.getFullName(),
                "SUCCESS"
        );
        return toDto(lead);
    }

    @Transactional
    public ConvertLeadResponse convert(ZeniusUserPrincipal principal, UUID leadId, ConvertLeadRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmLeadEntity lead = leadRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(leadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));

        if ("CONVERTED".equals(lead.getStatus())) {
            throw new IllegalStateException("Lead has already been converted");
        }

        String accountName = (req.accountName() != null && !req.accountName().isBlank())
                ? req.accountName().trim()
                : (lead.getCompanyName() != null ? lead.getCompanyName() : lead.getFullName() + " Account");

        CrmAccountEntity account = accountRepository
                .findFirstByOrganizationIdAndNameIgnoreCaseAndDeletedAtIsNull(orgId, accountName)
                .orElseGet(() -> {
                    CrmAccountEntity created = new CrmAccountEntity();
                    created.setOrganizationId(orgId);
                    created.setOwnerUserId(lead.getOwnerUserId());
                    created.setName(accountName);
                    created.setPhone(lead.getPhone());
                    created.setLifecycleStage("PROSPECT");
                    return accountRepository.save(created);
                });

        String[] parts = lead.getFullName().trim().split("\\s+", 2);
        CrmContactEntity contact = new CrmContactEntity();
        contact.setOrganizationId(orgId);
        contact.setAccountId(account.getId());
        contact.setOwnerUserId(lead.getOwnerUserId());
        contact.setFirstName(parts[0]);
        contact.setLastName(parts.length > 1 ? parts[1] : "-");
        contact.setEmail(lead.getEmail());
        contact.setPhone(lead.getPhone());
        contact.setJobTitle(lead.getJobTitle());
        contact = contactRepository.save(contact);

        UUID dealId = null;
        if (req.createDeal()) {
            var pipeline = pipelineService.getOrProvisionDefaultPipeline();
            var firstStage = pipeline.stages().getFirst();
            BigDecimal amount = req.dealAmount() != null
                    ? req.dealAmount()
                    : (lead.getEstimatedValue() != null ? lead.getEstimatedValue() : BigDecimal.ZERO);

            CrmDealEntity deal = new CrmDealEntity();
            deal.setOrganizationId(orgId);
            deal.setPipelineId(pipeline.id());
            deal.setStageId(firstStage.id());
            deal.setAccountId(account.getId());
            deal.setContactId(contact.getId());
            deal.setOwnerUserId(lead.getOwnerUserId());
            deal.setTitle(req.dealTitle() != null && !req.dealTitle().isBlank()
                    ? req.dealTitle().trim()
                    : accountName + " Opportunity");
            deal.setAmount(amount);
            deal.setProbability(firstStage.probability());
            deal.setExpectedRevenue(amount
                    .multiply(BigDecimal.valueOf(firstStage.probability()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            deal.setExpectedCloseDate(LocalDate.now().plusDays(30));
            deal = dealRepository.save(deal);
            dealId = deal.getId();
        }

        lead.setStatus("CONVERTED");
        lead.setConvertedAccountId(account.getId());
        lead.setConvertedContactId(contact.getId());
        lead.setConvertedAt(Instant.now());
        leadRepository.save(lead);

        CrmActivityEntity activity = new CrmActivityEntity();
        activity.setOrganizationId(orgId);
        activity.setOwnerUserId(principal.getUserId());
        activity.setActivityType("NOTE");
        activity.setSubject("Lead converted to Account & Contact");
        activity.setDescription("Converted lead " + lead.getFullName() + " into account " + account.getName());
        activity.setRelatedEntityType("ACCOUNT");
        activity.setRelatedEntityId(account.getId());
        activity.setStatus("COMPLETED");
        activity.setCompletedAt(Instant.now());
        activityRepository.save(activity);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_LEAD_CONVERTED",
                "CRM_LEAD",
                lead.getId().toString(),
                "Converted lead " + lead.getFullName() + " to account " + account.getName(),
                "SUCCESS"
        );

        return new ConvertLeadResponse(lead.getId(), account.getId(), contact.getId(), dealId);
    }

    @Transactional
    public void archive(ZeniusUserPrincipal principal, UUID leadId) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmLeadEntity lead = leadRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(leadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        lead.setDeletedAt(Instant.now());
        leadRepository.save(lead);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_LEAD_ARCHIVED",
                "CRM_LEAD",
                lead.getId().toString(),
                "Archived lead " + lead.getFullName(),
                "SUCCESS"
        );
    }

    @Transactional
    public ImportResult importCsv(ZeniusUserPrincipal principal, String csvContent) {
        UUID orgId = TenantContext.requireOrganizationId();
        String[] lines = csvContent.split("\\r?\\n");
        int imported = 0;
        int skipped = 0;
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || (i == 0 && line.toLowerCase().startsWith("fullname,"))) {
                continue;
            }
            String[] cols = line.split(",", -1);
            String fullName = cols[0].trim();
            if (fullName.isEmpty()) {
                skipped++;
                continue;
            }
            String email = cols.length > 1 ? norm(cols[1]) : null;
            if (email != null && leadRepository.existsByOrganizationIdAndEmailIgnoreCaseAndDeletedAtIsNull(orgId, email)) {
                skipped++;
                continue;
            }
            String phone = cols.length > 2 ? norm(cols[2]) : null;
            String company = cols.length > 3 ? norm(cols[3]) : null;
            String source = cols.length > 4 && !cols[4].isBlank() ? cols[4].trim().toUpperCase() : "IMPORT";
            BigDecimal est = null;
            if (cols.length > 5 && !cols[5].isBlank()) {
                try {
                    est = new BigDecimal(cols[5].trim());
                } catch (NumberFormatException ignored) {
                    est = null;
                }
            }
            LeadDto dto = create(
                    principal,
                    new LeadMutationRequest(fullName, email, phone, company, null, source, "NEW", est, null, null)
            );
            createdIds.add(dto.id());
            imported++;
        }
        return new ImportResult(imported, skipped, createdIds);
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<CrmLeadEntity> leads = leadRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(orgId);
        StringBuilder sb = new StringBuilder("fullName,email,phone,companyName,source,status,leadScore,estimatedValue\n");
        for (CrmLeadEntity l : leads) {
            sb.append(escapeCsv(l.getFullName())).append(',')
                    .append(escapeCsv(l.getEmail())).append(',')
                    .append(escapeCsv(l.getPhone())).append(',')
                    .append(escapeCsv(l.getCompanyName())).append(',')
                    .append(escapeCsv(l.getSource())).append(',')
                    .append(escapeCsv(l.getStatus())).append(',')
                    .append(l.getLeadScore()).append(',')
                    .append(l.getEstimatedValue() != null ? l.getEstimatedValue().toPlainString() : "")
                    .append('\n');
        }
        return sb.toString();
    }

    private int computeLeadScore(CrmLeadEntity lead) {
        int score = 20;
        if (lead.getEmail() != null) score += 15;
        if (lead.getPhone() != null) score += 10;
        if (lead.getCompanyName() != null) score += 15;
        if (lead.getEstimatedValue() != null && lead.getEstimatedValue().compareTo(BigDecimal.valueOf(10000)) >= 0) {
            score += 20;
        }
        if ("REFERRAL".equals(lead.getSource()) || "PARTNER".equals(lead.getSource())) {
            score += 15;
        }
        if ("QUALIFIED".equals(lead.getStatus())) {
            score += 15;
        }
        return Math.min(100, score);
    }

    private LeadDto toDto(CrmLeadEntity l) {
        return new LeadDto(
                l.getId(),
                l.getOwnerUserId(),
                l.getFullName(),
                l.getEmail(),
                l.getPhone(),
                l.getCompanyName(),
                l.getJobTitle(),
                l.getSource(),
                l.getStatus(),
                l.getLeadScore(),
                l.getEstimatedValue(),
                l.getNotes(),
                l.getConvertedAccountId(),
                l.getConvertedContactId(),
                l.getConvertedAt(),
                l.getCreatedAt()
        );
    }

    private String norm(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        String cleaned = val.replace("\"", "\"\"");
        return cleaned.contains(",") ? "\"" + cleaned + "\"" : cleaned;
    }

    public record LeadMutationRequest(
            String fullName,
            String email,
            String phone,
            String companyName,
            String jobTitle,
            String source,
            String status,
            BigDecimal estimatedValue,
            String notes,
            UUID ownerUserId
    ) {
    }

    public record ConvertLeadRequest(
            String accountName,
            boolean createDeal,
            String dealTitle,
            BigDecimal dealAmount
    ) {
    }

    public record ConvertLeadResponse(
            UUID leadId,
            UUID accountId,
            UUID contactId,
            UUID dealId
    ) {
    }

    public record ImportResult(int imported, int skipped, List<UUID> createdIds) {
    }

    public record LeadDto(
            UUID id,
            UUID ownerUserId,
            String fullName,
            String email,
            String phone,
            String companyName,
            String jobTitle,
            String source,
            String status,
            Integer leadScore,
            BigDecimal estimatedValue,
            String notes,
            UUID convertedAccountId,
            UUID convertedContactId,
            Instant convertedAt,
            Instant createdAt
    ) {
    }
}

