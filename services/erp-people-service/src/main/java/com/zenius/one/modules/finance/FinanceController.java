package com.zenius.one.modules.finance;

import com.zenius.one.modules.finance.FinanceService.AccountDto;
import com.zenius.one.modules.finance.FinanceService.BillDto;
import com.zenius.one.modules.finance.FinanceService.CreateAccountRequest;
import com.zenius.one.modules.finance.FinanceService.CreateBillRequest;
import com.zenius.one.modules.finance.FinanceService.CreateInvoiceRequest;
import com.zenius.one.modules.finance.FinanceService.CreateJournalRequest;
import com.zenius.one.modules.finance.FinanceService.FinancialReportsDto;
import com.zenius.one.modules.finance.FinanceService.InvoiceDto;
import com.zenius.one.modules.finance.FinanceService.JournalEntryDto;
import com.zenius.one.modules.finance.FinanceService.JournalLineInput;
import com.zenius.one.modules.finance.FinanceService.RecordPaymentRequest;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/v1/finance")
@Validated
@Tag(name = "Finance & Accounting", description = "Chart of accounts, double-entry journals, AR/AP, India GST/TDS, and financial statements")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/accounts")
    @PreAuthorize("@authz.hasPermission('finance:read')")
    public ApiResponse<List<AccountDto>> listAccounts() {
        return ApiResponse.ok(financeService.listAccounts());
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('finance:write')")
    public ApiResponse<AccountDto> createAccount(@Valid @RequestBody AccountBody body) {
        return ApiResponse.ok(financeService.createAccount(
                new CreateAccountRequest(body.code(), body.name(), body.accountType(), body.currency())
        ));
    }

    @GetMapping("/journals")
    @PreAuthorize("@authz.hasPermission('finance:read')")
    public ApiResponse<List<JournalEntryDto>> listJournals(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<JournalEntryDto> res = financeService.listJournals(PageRequest.of(page, size));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/journals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('finance:write')")
    public ApiResponse<JournalEntryDto> postJournal(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody JournalBody body
    ) {
        List<JournalLineInput> lines = body.lines().stream()
                .map(l -> new JournalLineInput(l.accountId(), l.description(), l.debitAmount(), l.creditAmount()))
                .toList();
        return ApiResponse.ok(financeService.postJournal(
                principal,
                new CreateJournalRequest(body.entryDate(), body.reference(), body.description(), lines)
        ));
    }

    @GetMapping("/invoices")
    @PreAuthorize("@authz.hasPermission('finance:read')")
    public ApiResponse<List<InvoiceDto>> listInvoices(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<InvoiceDto> res = financeService.listInvoices(PageRequest.of(page, size));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('finance:write')")
    public ApiResponse<InvoiceDto> createInvoice(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody InvoiceBody body
    ) {
        return ApiResponse.ok(financeService.createInvoice(
                principal,
                new CreateInvoiceRequest(
                        body.customerName(), body.customerEmail(), body.currency(), body.issueDate(),
                        body.dueDate(), body.subtotal(), body.gstRate(), body.tdsRate(), body.notes()
                )
        ));
    }

    @PostMapping("/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('finance:write')")
    public ApiResponse<InvoiceDto> recordInvoicePayment(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PaymentBody body
    ) {
        return ApiResponse.ok(financeService.recordInvoicePayment(
                principal,
                id,
                new RecordPaymentRequest(body.amount(), body.paymentDate(), body.method(), body.referenceNote())
        ));
    }

    @GetMapping("/bills")
    @PreAuthorize("@authz.hasPermission('finance:read')")
    public ApiResponse<List<BillDto>> listBills(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<BillDto> res = financeService.listBills(PageRequest.of(page, size));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/bills")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('finance:write')")
    public ApiResponse<BillDto> createBill(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody BillBody body
    ) {
        return ApiResponse.ok(financeService.createBill(
                principal,
                new CreateBillRequest(body.vendorName(), body.billDate(), body.dueDate(), body.subtotal(), body.tdsRate())
        ));
    }

    @PostMapping("/bills/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('finance:write')")
    public ApiResponse<BillDto> payBill(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PaymentBody body
    ) {
        return ApiResponse.ok(financeService.payBill(
                principal,
                id,
                new RecordPaymentRequest(body.amount(), body.paymentDate(), body.method(), body.referenceNote())
        ));
    }

    @GetMapping("/reports")
    @PreAuthorize("@authz.hasPermission('finance:read')")
    public ApiResponse<FinancialReportsDto> reports() {
        return ApiResponse.ok(financeService.getFinancialReports());
    }

    public record AccountBody(@NotBlank String code, @NotBlank String name, @NotBlank String accountType, String currency) {
    }

    public record JournalLineBody(@NotNull UUID accountId, String description, BigDecimal debitAmount, BigDecimal creditAmount) {
    }

    public record JournalBody(LocalDate entryDate, String reference, @NotBlank String description, @NotEmpty @Valid List<JournalLineBody> lines) {
    }

    public record InvoiceBody(@NotBlank String customerName, String customerEmail, String currency, LocalDate issueDate, LocalDate dueDate, @NotNull @DecimalMin("0.01") BigDecimal subtotal, BigDecimal gstRate, BigDecimal tdsRate, String notes) {
    }

    public record BillBody(@NotBlank String vendorName, LocalDate billDate, LocalDate dueDate, @NotNull @DecimalMin("0.01") BigDecimal subtotal, BigDecimal tdsRate) {
    }

    public record PaymentBody(@NotNull @DecimalMin("0.01") BigDecimal amount, LocalDate paymentDate, String method, String referenceNote) {
    }
}

