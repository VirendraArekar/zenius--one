package com.zenius.one.modules.crm;

import com.zenius.one.modules.crm.CrmAccountAndContactService.AccountDto;
import com.zenius.one.modules.crm.CrmAccountAndContactService.AccountMutationRequest;
import com.zenius.one.modules.crm.CrmAccountAndContactService.ContactDto;
import com.zenius.one.modules.crm.CrmAccountAndContactService.ContactMutationRequest;
import com.zenius.one.modules.crm.CrmDealService.CreateDealRequest;
import com.zenius.one.modules.crm.CrmDealService.CrmDashboardDto;
import com.zenius.one.modules.crm.CrmDealService.DealDto;
import com.zenius.one.modules.crm.CrmDealService.MoveDealStageRequest;
import com.zenius.one.modules.crm.CrmLeadService.ConvertLeadRequest;
import com.zenius.one.modules.crm.CrmLeadService.ConvertLeadResponse;
import com.zenius.one.modules.crm.CrmLeadService.ImportResult;
import com.zenius.one.modules.crm.CrmLeadService.LeadDto;
import com.zenius.one.modules.crm.CrmLeadService.LeadMutationRequest;
import com.zenius.one.modules.crm.CrmPipelineService.PipelineWithStagesDto;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crm")
@Validated
@Tag(name = "CRM Core", description = "Leads, Contacts, Accounts, Pipelines, Deals, and Forecasting")
public class CrmController {

    private final CrmLeadService leadService;
    private final CrmAccountAndContactService accountAndContactService;
    private final CrmPipelineService pipelineService;
    private final CrmDealService dealService;

    public CrmController(
            CrmLeadService leadService,
            CrmAccountAndContactService accountAndContactService,
            CrmPipelineService pipelineService,
            CrmDealService dealService
    ) {
        this.leadService = leadService;
        this.accountAndContactService = accountAndContactService;
        this.pipelineService = pipelineService;
        this.dealService = dealService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@authz.hasPermission('crm:deal:read')")
    @Operation(summary = "Get live CRM pipeline funnel, forecast, and lead conversion metrics")
    public ApiResponse<CrmDashboardDto> dashboard() {
        return ApiResponse.ok(dealService.getDashboardSummary());
    }

    // Leads
    @GetMapping("/leads")
    @PreAuthorize("@authz.hasPermission('crm:lead:read')")
    public ApiResponse<List<LeadDto>> listLeads(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<LeadDto> res = leadService.list(status, source, q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('crm:lead:write')")
    public ApiResponse<LeadDto> createLead(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody LeadBody body
    ) {
        return ApiResponse.ok(leadService.create(
                principal,
                new LeadMutationRequest(
                        body.fullName(), body.email(), body.phone(), body.companyName(), body.jobTitle(),
                        body.source(), body.status(), body.estimatedValue(), body.notes(), body.ownerUserId()
                )
        ));
    }

    @PutMapping("/leads/{id}")
    @PreAuthorize("@authz.hasPermission('crm:lead:write')")
    public ApiResponse<LeadDto> updateLead(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody LeadBody body
    ) {
        return ApiResponse.ok(leadService.update(
                principal,
                id,
                new LeadMutationRequest(
                        body.fullName(), body.email(), body.phone(), body.companyName(), body.jobTitle(),
                        body.source(), body.status(), body.estimatedValue(), body.notes(), body.ownerUserId()
                )
        ));
    }

    @PostMapping("/leads/{id}/convert")
    @PreAuthorize("@authz.hasPermission('crm:lead:convert')")
    public ApiResponse<ConvertLeadResponse> convertLead(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) ConvertBody body
    ) {
        ConvertLeadRequest req = body != null
                ? new ConvertLeadRequest(body.accountName(), body.createDeal(), body.dealTitle(), body.dealAmount())
                : new ConvertLeadRequest(null, true, null, null);
        return ApiResponse.ok(leadService.convert(principal, id, req));
    }

    @DeleteMapping("/leads/{id}")
    @PreAuthorize("@authz.hasPermission('crm:lead:write')")
    public ApiResponse<Map<String, Boolean>> archiveLead(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        leadService.archive(principal, id);
        return ApiResponse.ok(Map.of("archived", true));
    }

    @PostMapping("/leads/import")
    @PreAuthorize("@authz.hasPermission('crm:lead:write')")
    public ApiResponse<ImportResult> importLeads(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody CsvImportBody body
    ) {
        return ApiResponse.ok(leadService.importCsv(principal, body.csv()));
    }

    @GetMapping("/leads/export")
    @PreAuthorize("@authz.hasPermission('crm:lead:read')")
    public ResponseEntity<String> exportLeads() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(leadService.exportCsv());
    }

    // Accounts
    @GetMapping("/accounts")
    @PreAuthorize("@authz.hasPermission('crm:account:read')")
    public ApiResponse<List<AccountDto>> listAccounts(
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<AccountDto> res = accountAndContactService.listAccounts(stage, q, PageRequest.of(page, size, Sort.by("name").ascending()));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('crm:account:write')")
    public ApiResponse<AccountDto> createAccount(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody AccountBody body
    ) {
        return ApiResponse.ok(accountAndContactService.createAccount(
                principal,
                new AccountMutationRequest(
                        body.parentAccountId(), body.name(), body.industry(), body.website(), body.phone(),
                        body.annualRevenue(), body.employeeCount(), body.lifecycleStage(), body.billingCity(), body.billingCountry()
                )
        ));
    }

    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("@authz.hasPermission('crm:account:write')")
    public ApiResponse<Map<String, Boolean>> archiveAccount(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        accountAndContactService.archiveAccount(principal, id);
        return ApiResponse.ok(Map.of("archived", true));
    }

    // Contacts
    @GetMapping("/contacts")
    @PreAuthorize("@authz.hasPermission('crm:contact:read')")
    public ApiResponse<List<ContactDto>> listContacts(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<ContactDto> res = accountAndContactService.listContacts(accountId, q, PageRequest.of(page, size, Sort.by("firstName").ascending()));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('crm:contact:write')")
    public ApiResponse<ContactDto> createContact(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody ContactBody body
    ) {
        return ApiResponse.ok(accountAndContactService.createContact(
                principal,
                new ContactMutationRequest(
                        body.accountId(), body.firstName(), body.lastName(), body.email(), body.phone(),
                        body.jobTitle(), body.department(), body.segment()
                )
        ));
    }

    // Pipelines & Deals
    @GetMapping("/pipelines")
    @PreAuthorize("@authz.hasPermission('crm:deal:read')")
    public ApiResponse<List<PipelineWithStagesDto>> listPipelines() {
        return ApiResponse.ok(pipelineService.listPipelines());
    }

    @GetMapping("/deals")
    @PreAuthorize("@authz.hasPermission('crm:deal:read')")
    public ApiResponse<List<DealDto>> listDeals(
            @RequestParam(required = false) UUID pipelineId,
            @RequestParam(required = false) UUID stageId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        Page<DealDto> res = dealService.listDeals(pipelineId, stageId, q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/deals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('crm:deal:write')")
    public ApiResponse<DealDto> createDeal(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody DealBody body
    ) {
        return ApiResponse.ok(dealService.createDeal(
                principal,
                new CreateDealRequest(
                        body.pipelineId(), body.stageId(), body.accountId(), body.contactId(),
                        body.title(), body.amount(), body.expectedCloseDate()
                )
        ));
    }

    @PatchMapping("/deals/{id}/stage")
    @PreAuthorize("@authz.hasPermission('crm:deal:write')")
    public ApiResponse<DealDto> moveDealStage(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody MoveStageBody body
    ) {
        return ApiResponse.ok(dealService.moveStage(principal, id, new MoveDealStageRequest(body.stageId(), body.lostReason())));
    }

    public record LeadBody(
            @NotBlank String fullName,
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

    public record ConvertBody(String accountName, boolean createDeal, String dealTitle, BigDecimal dealAmount) {
    }

    public record CsvImportBody(@NotBlank String csv) {
    }

    public record AccountBody(
            UUID parentAccountId,
            @NotBlank String name,
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

    public record ContactBody(
            UUID accountId,
            @NotBlank String firstName,
            @NotBlank String lastName,
            String email,
            String phone,
            String jobTitle,
            String department,
            String segment
    ) {
    }

    public record DealBody(
            UUID pipelineId,
            UUID stageId,
            UUID accountId,
            UUID contactId,
            @NotBlank String title,
            BigDecimal amount,
            LocalDate expectedCloseDate
    ) {
    }

    public record MoveStageBody(@NotNull UUID stageId, String lostReason) {
    }
}

