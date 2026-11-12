package com.zenius.one.modules.crm;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmAccountAndContactService {

    private final CrmAccountRepository accountRepository;
    private final CrmContactRepository contactRepository;
    private final AuditLogService auditLogService;

    public CrmAccountAndContactService(
            CrmAccountRepository accountRepository,
            CrmContactRepository contactRepository,
            AuditLogService auditLogService
    ) {
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<AccountDto> listAccounts(String stage, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return accountRepository.search(orgId, norm(stage), norm(query), pageable).map(this::toAccountDto);
    }

    @Transactional
    public AccountDto createAccount(ZeniusUserPrincipal principal, AccountMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (req.parentAccountId() != null
                && accountRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(req.parentAccountId(), orgId).isEmpty()) {
            throw new IllegalArgumentException("Parent account not found in current organization");
        }

        CrmAccountEntity entity = new CrmAccountEntity();
        entity.setOrganizationId(orgId);
        entity.setParentAccountId(req.parentAccountId());
        entity.setOwnerUserId(principal.getUserId());
        entity.setName(req.name().trim());
        entity.setIndustry(norm(req.industry()));
        entity.setWebsite(norm(req.website()));
        entity.setPhone(norm(req.phone()));
        entity.setAnnualRevenue(req.annualRevenue());
        entity.setEmployeeCount(req.employeeCount());
        entity.setLifecycleStage(req.lifecycleStage() != null && !req.lifecycleStage().isBlank()
                ? req.lifecycleStage().trim().toUpperCase()
                : "PROSPECT");
        entity.setBillingCity(norm(req.billingCity()));
        entity.setBillingCountry(norm(req.billingCountry()));
        entity = accountRepository.save(entity);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_ACCOUNT_CREATED",
                "CRM_ACCOUNT",
                entity.getId().toString(),
                "Created CRM account " + entity.getName(),
                "SUCCESS"
        );
        return toAccountDto(entity);
    }

    @Transactional
    public void archiveAccount(ZeniusUserPrincipal principal, UUID accountId) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmAccountEntity entity = accountRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(accountId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        entity.setDeletedAt(Instant.now());
        accountRepository.save(entity);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_ACCOUNT_ARCHIVED",
                "CRM_ACCOUNT",
                entity.getId().toString(),
                "Archived CRM account " + entity.getName(),
                "SUCCESS"
        );
    }

    @Transactional(readOnly = true)
    public Page<ContactDto> listContacts(UUID accountId, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return contactRepository.search(orgId, accountId, norm(query), pageable).map(this::toContactDto);
    }

    @Transactional
    public ContactDto createContact(ZeniusUserPrincipal principal, ContactMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (req.accountId() != null
                && accountRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(req.accountId(), orgId).isEmpty()) {
            throw new IllegalArgumentException("Account does not belong to current organization");
        }

        CrmContactEntity contact = new CrmContactEntity();
        contact.setOrganizationId(orgId);
        contact.setAccountId(req.accountId());
        contact.setOwnerUserId(principal.getUserId());
        contact.setFirstName(req.firstName().trim());
        contact.setLastName(req.lastName().trim());
        contact.setEmail(norm(req.email()));
        contact.setPhone(norm(req.phone()));
        contact.setJobTitle(norm(req.jobTitle()));
        contact.setDepartment(norm(req.department()));
        contact.setSegment(norm(req.segment()));
        contact = contactRepository.save(contact);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_CONTACT_CREATED",
                "CRM_CONTACT",
                contact.getId().toString(),
                "Created contact " + contact.getFirstName() + " " + contact.getLastName(),
                "SUCCESS"
        );
        return toContactDto(contact);
    }

    @Transactional
    public void archiveContact(ZeniusUserPrincipal principal, UUID contactId) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmContactEntity contact = contactRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(contactId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found: " + contactId));
        contact.setDeletedAt(Instant.now());
        contactRepository.save(contact);
    }

    private AccountDto toAccountDto(CrmAccountEntity a) {
        return new AccountDto(
                a.getId(),
                a.getParentAccountId(),
                a.getOwnerUserId(),
                a.getName(),
                a.getIndustry(),
                a.getWebsite(),
                a.getPhone(),
                a.getAnnualRevenue(),
                a.getEmployeeCount(),
                a.getLifecycleStage(),
                a.getBillingCity(),
                a.getBillingCountry(),
                a.getCreatedAt()
        );
    }

    private ContactDto toContactDto(CrmContactEntity c) {
        return new ContactDto(
                c.getId(),
                c.getAccountId(),
                c.getOwnerUserId(),
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getPhone(),
                c.getJobTitle(),
                c.getDepartment(),
                c.getSegment(),
                c.getCreatedAt()
        );
    }

    private String norm(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    public record AccountMutationRequest(
            UUID parentAccountId,
            String name,
            String industry,
            String website,
            String phone,
            BigDecimal annualRevenue,
            Integer employeeCount,
            String lifecycleStage,
            String billingCity,
            String billingCountry
    ) {
    }

    public record AccountDto(
            UUID id,
            UUID parentAccountId,
            UUID ownerUserId,
            String name,
            String industry,
            String website,
            String phone,
            BigDecimal annualRevenue,
            Integer employeeCount,
            String lifecycleStage,
            String billingCity,
            String billingCountry,
            Instant createdAt
    ) {
    }

    public record ContactMutationRequest(
            UUID accountId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String jobTitle,
            String department,
            String segment
    ) {
    }

    public record ContactDto(
            UUID id,
            UUID accountId,
            UUID ownerUserId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String jobTitle,
            String department,
            String segment,
            Instant createdAt
    ) {
    }
}

