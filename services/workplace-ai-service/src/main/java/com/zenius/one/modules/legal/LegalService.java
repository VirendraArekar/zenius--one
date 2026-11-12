package com.zenius.one.modules.legal;

import com.zenius.one.modules.documents.DocFileRepository;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalService {

    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "IN_REVIEW", "APPROVED", "EXECUTED", "EXPIRED");

    private final LegContractRepository contractRepository;
    private final DocFileRepository docFileRepository;
    private final AuditLogService auditLogService;

    public LegalService(
            LegContractRepository contractRepository,
            DocFileRepository docFileRepository,
            AuditLogService auditLogService) {
        this.contractRepository = contractRepository;
        this.docFileRepository = docFileRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ContractSummary> listContracts() {
        UUID orgId = TenantContext.requireOrganizationId();
        return contractRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(ContractSummary::from)
                .toList();
    }

    @Transactional
    public ContractSummary createContract(ZeniusUserPrincipal principal, @Valid CreateContractRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (request.expirationDate().isBefore(request.effectiveDate())) {
            throw new IllegalArgumentException("Expiration date cannot be before effective date");
        }
        if (request.documentId() != null) {
            docFileRepository
                    .findByIdAndOrganizationId(request.documentId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Linked document not found"));
        }

        long seq = contractRepository.countByOrganizationId(orgId) + 1;
        LegContractEntity contract = new LegContractEntity();
        contract.setOrganizationId(orgId);
        contract.setContractNumber(String.format("CTR-%05d", seq));
        contract.setTitle(request.title().trim());
        contract.setCounterpartyName(request.counterpartyName().trim());
        contract.setContractType(request.contractType().trim().toUpperCase());
        contract.setValueAmount(request.valueAmount() == null ? BigDecimal.ZERO : request.valueAmount());
        contract.setEffectiveDate(request.effectiveDate());
        contract.setExpirationDate(request.expirationDate());
        contract.setStatus("DRAFT");
        contract.setDocumentId(request.documentId());
        LegContractEntity saved = contractRepository.save(contract);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CONTRACT_CREATED",
                "LEG_CONTRACT",
                saved.getId().toString(),
                "Created legal contract " + saved.getContractNumber() + " (" + saved.getTitle() + ")",
                "SUCCESS");

        return ContractSummary.from(saved);
    }

    @Transactional
    public ContractSummary updateContractStatus(
            ZeniusUserPrincipal principal, UUID contractId, @Valid UpdateStatusRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        LegContractEntity contract = contractRepository
                .findByIdAndOrganizationId(contractId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        String nextStatus = request.status().trim().toUpperCase();
        if (!VALID_STATUSES.contains(nextStatus)) {
            throw new IllegalArgumentException("Invalid contract status: " + request.status());
        }

        contract.setStatus(nextStatus);
        LegContractEntity saved = contractRepository.save(contract);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CONTRACT_STATUS_UPDATED",
                "LEG_CONTRACT",
                saved.getId().toString(),
                "Transitioned contract " + saved.getContractNumber() + " to " + nextStatus,
                "SUCCESS");

        return ContractSummary.from(saved);
    }

    public record CreateContractRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 160) String counterpartyName,
            @NotBlank @Size(max = 48) String contractType,
            @DecimalMin("0.00") BigDecimal valueAmount,
            @NotNull LocalDate effectiveDate,
            @NotNull LocalDate expirationDate,
            UUID documentId) {}

    public record UpdateStatusRequest(@NotBlank String status) {}

    public record ContractSummary(
            UUID id,
            String contractNumber,
            String title,
            String counterpartyName,
            String contractType,
            BigDecimal valueAmount,
            LocalDate effectiveDate,
            LocalDate expirationDate,
            String status,
            UUID documentId,
            Instant createdAt) {
        public static ContractSummary from(LegContractEntity entity) {
            return new ContractSummary(
                    entity.getId(),
                    entity.getContractNumber(),
                    entity.getTitle(),
                    entity.getCounterpartyName(),
                    entity.getContractType(),
                    entity.getValueAmount(),
                    entity.getEffectiveDate(),
                    entity.getExpirationDate(),
                    entity.getStatus(),
                    entity.getDocumentId(),
                    entity.getCreatedAt());
        }
    }
}

