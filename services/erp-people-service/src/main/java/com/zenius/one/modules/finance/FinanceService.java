package com.zenius.one.modules.finance;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {

    private final FinAccountRepository accountRepository;
    private final FinJournalEntryRepository journalRepository;
    private final FinJournalLineRepository journalLineRepository;
    private final FinInvoiceRepository invoiceRepository;
    private final FinBillRepository billRepository;
    private final FinPaymentRepository paymentRepository;
    private final AuditLogService auditLogService;

    public FinanceService(
            FinAccountRepository accountRepository,
            FinJournalEntryRepository journalRepository,
            FinJournalLineRepository journalLineRepository,
            FinInvoiceRepository invoiceRepository,
            FinBillRepository billRepository,
            FinPaymentRepository paymentRepository,
            AuditLogService auditLogService
    ) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
        this.journalLineRepository = journalLineRepository;
        this.invoiceRepository = invoiceRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public List<AccountDto> listAccounts() {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultChartOfAccounts(orgId);
        return accountRepository.findByOrganizationIdOrderByCodeAsc(orgId).stream()
                .map(a -> new AccountDto(a.getId(), a.getCode(), a.getName(), a.getAccountType(), a.getCurrency(), a.isSystemAccount(), a.isActive()))
                .toList();
    }

    @Transactional
    public AccountDto createAccount(CreateAccountRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultChartOfAccounts(orgId);
        String code = req.code().trim().toUpperCase();
        if (accountRepository.existsByOrganizationIdAndCodeIgnoreCase(orgId, code)) {
            throw new IllegalStateException("Account code already exists: " + code);
        }
        FinAccountEntity a = new FinAccountEntity();
        a.setOrganizationId(orgId);
        a.setCode(code);
        a.setName(req.name().trim());
        a.setAccountType(req.accountType().trim().toUpperCase());
        a.setCurrency(req.currency() != null && !req.currency().isBlank() ? req.currency().trim().toUpperCase() : "USD");
        a.setSystemAccount(false);
        a.setActive(true);
        a = accountRepository.save(a);
        return new AccountDto(a.getId(), a.getCode(), a.getName(), a.getAccountType(), a.getCurrency(), a.isSystemAccount(), a.isActive());
    }

    @Transactional(readOnly = true)
    public Page<JournalEntryDto> listJournals(Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return journalRepository.findByOrganizationIdOrderByEntryDateDescCreatedAtDesc(orgId, pageable)
                .map(j -> {
                    List<JournalLineDto> lines = journalLineRepository.findByOrganizationIdAndJournalEntryId(orgId, j.getId())
                            .stream()
                            .map(l -> new JournalLineDto(l.getId(), l.getAccountId(), l.getDescription(), l.getDebitAmount(), l.getCreditAmount()))
                            .toList();
                    return new JournalEntryDto(j.getId(), j.getEntryNumber(), j.getEntryDate(), j.getReference(), j.getDescription(), j.getStatus(), j.getTotalDebit(), j.getTotalCredit(), lines);
                });
    }

    @Transactional
    public JournalEntryDto postJournal(ZeniusUserPrincipal principal, CreateJournalRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultChartOfAccounts(orgId);
        if (req.lines() == null || req.lines().size() < 2) {
            throw new IllegalArgumentException("Double-entry journal requires at least 2 lines");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (JournalLineInput line : req.lines()) {
            if (accountRepository.findByIdAndOrganizationId(line.accountId(), orgId).isEmpty()) {
                throw new IllegalArgumentException("Account does not belong to current organization: " + line.accountId());
            }
            BigDecimal dr = line.debitAmount() != null ? line.debitAmount() : BigDecimal.ZERO;
            BigDecimal cr = line.creditAmount() != null ? line.creditAmount() : BigDecimal.ZERO;
            if (dr.compareTo(BigDecimal.ZERO) < 0 || cr.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Debit and credit amounts must be non-negative");
            }
            totalDebit = totalDebit.add(dr);
            totalCredit = totalCredit.add(cr);
        }

        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0 || totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(
                    "Unbalanced journal entry: total debits (" + totalDebit + ") must equal total credits (" + totalCredit + ")"
            );
        }

        long seq = journalRepository.countByOrganizationId(orgId) + 1;
        FinJournalEntryEntity entry = new FinJournalEntryEntity();
        entry.setOrganizationId(orgId);
        entry.setEntryNumber(String.format("JE-%05d", seq));
        entry.setEntryDate(req.entryDate() != null ? req.entryDate() : LocalDate.now());
        entry.setReference(req.reference());
        entry.setDescription(req.description().trim());
        entry.setStatus("POSTED");
        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);
        entry = journalRepository.save(entry);

        List<JournalLineDto> savedLines = new ArrayList<>();
        for (JournalLineInput lineIn : req.lines()) {
            FinJournalLineEntity line = new FinJournalLineEntity();
            line.setOrganizationId(orgId);
            line.setJournalEntryId(entry.getId());
            line.setAccountId(lineIn.accountId());
            line.setDescription(lineIn.description());
            line.setDebitAmount(lineIn.debitAmount() != null ? lineIn.debitAmount() : BigDecimal.ZERO);
            line.setCreditAmount(lineIn.creditAmount() != null ? lineIn.creditAmount() : BigDecimal.ZERO);
            line = journalLineRepository.save(line);
            savedLines.add(new JournalLineDto(line.getId(), line.getAccountId(), line.getDescription(), line.getDebitAmount(), line.getCreditAmount()));
        }

        if (principal != null) {
            auditLogService.record(orgId, principal.getUserId(), principal.getEmail(),
                    "FIN_JOURNAL_POSTED", "FIN_JOURNAL", entry.getId().toString(),
                    "Posted journal " + entry.getEntryNumber() + " (" + totalDebit + ")", "SUCCESS");
        }
        return new JournalEntryDto(entry.getId(), entry.getEntryNumber(), entry.getEntryDate(), entry.getReference(), entry.getDescription(), entry.getStatus(), entry.getTotalDebit(), entry.getTotalCredit(), savedLines);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceDto> listInvoices(Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return invoiceRepository.findByOrganizationIdOrderByIssueDateDescCreatedAtDesc(orgId, pageable).map(this::toInvoiceDto);
    }

    @Transactional
    public InvoiceDto createInvoice(ZeniusUserPrincipal principal, CreateInvoiceRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultChartOfAccounts(orgId);

        BigDecimal subtotal = req.subtotal();
        BigDecimal gstRate = req.gstRate() != null ? req.gstRate() : BigDecimal.ZERO;
        BigDecimal tdsRate = req.tdsRate() != null ? req.tdsRate() : BigDecimal.ZERO;
        BigDecimal gstAmount = subtotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal tdsAmount = subtotal.multiply(tdsRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(gstAmount);

        long seq = invoiceRepository.countByOrganizationId(orgId) + 1;
        FinInvoiceEntity inv = new FinInvoiceEntity();
        inv.setOrganizationId(orgId);
        inv.setInvoiceNumber(String.format("INV-%05d", seq));
        inv.setCustomerName(req.customerName().trim());
        inv.setCustomerEmail(req.customerEmail());
        inv.setCurrency(req.currency() != null && !req.currency().isBlank() ? req.currency().trim().toUpperCase() : "USD");
        inv.setIssueDate(req.issueDate() != null ? req.issueDate() : LocalDate.now());
        inv.setDueDate(req.dueDate() != null ? req.dueDate() : LocalDate.now().plusDays(30));
        inv.setSubtotal(subtotal);
        inv.setGstRate(gstRate);
        inv.setGstAmount(gstAmount);
        inv.setTdsRate(tdsRate);
        inv.setTdsAmount(tdsAmount);
        inv.setTotalAmount(totalAmount);
        inv.setBalanceDue(totalAmount);
        inv.setStatus("SENT");
        inv.setNotes(req.notes());
        inv = invoiceRepository.save(inv);

        // Post double-entry GL: DR Accounts Receivable (total), CR Revenue (subtotal), CR GST Payable (gstAmount)
        UUID arAccount = requireSystemAccountId(orgId, "1100");
        UUID revAccount = requireSystemAccountId(orgId, "4000");
        UUID gstAccount = requireSystemAccountId(orgId, "2100");
        List<JournalLineInput> lines = new ArrayList<>();
        lines.add(new JournalLineInput(arAccount, "AR for " + inv.getInvoiceNumber(), totalAmount, BigDecimal.ZERO));
        lines.add(new JournalLineInput(revAccount, "Revenue " + inv.getInvoiceNumber(), BigDecimal.ZERO, subtotal));
        if (gstAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new JournalLineInput(gstAccount, "GST Output " + inv.getInvoiceNumber(), BigDecimal.ZERO, gstAmount));
        }
        postJournal(principal, new CreateJournalRequest(inv.getIssueDate(), inv.getInvoiceNumber(), "Customer Invoice " + inv.getInvoiceNumber(), lines));

        return toInvoiceDto(inv);
    }

    @Transactional
    public InvoiceDto recordInvoicePayment(ZeniusUserPrincipal principal, UUID invoiceId, RecordPaymentRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultChartOfAccounts(orgId);
        FinInvoiceEntity inv = invoiceRepository.findByIdAndOrganizationId(invoiceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (req.amount().compareTo(BigDecimal.ZERO) <= 0 || req.amount().compareTo(inv.getBalanceDue()) > 0) {
            throw new IllegalArgumentException("Payment amount must be between 0.01 and remaining balance " + inv.getBalanceDue());
        }

        FinPaymentEntity payment = new FinPaymentEntity();
        payment.setOrganizationId(orgId);
        payment.setPaymentType("RECEIVABLE");
        payment.setReferenceId(inv.getId());
        payment.setPaymentDate(req.paymentDate() != null ? req.paymentDate() : LocalDate.now());
        payment.setAmount(req.amount());
        payment.setMethod(req.method() != null && !req.method().isBlank() ? req.method().trim().toUpperCase() : "BANK_TRANSFER");
        payment.setReferenceNote(req.referenceNote());
        paymentRepository.save(payment);

        BigDecimal remaining = inv.getBalanceDue().subtract(req.amount());
        inv.setBalanceDue(remaining);
        inv.setStatus(remaining.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIAL");
        inv = invoiceRepository.save(inv);

        UUID cashAccount = requireSystemAccountId(orgId, "1000");
        UUID arAccount = requireSystemAccountId(orgId, "1100");
        postJournal(principal, new CreateJournalRequest(
                payment.getPaymentDate(),
                inv.getInvoiceNumber(),
                "Payment received for " + inv.getInvoiceNumber(),
                List.of(
                        new JournalLineInput(cashAccount, "Cash receipt", req.amount(), BigDecimal.ZERO),
                        new JournalLineInput(arAccount, "Settle AR", BigDecimal.ZERO, req.amount())
                )
        ));

        return toInvoiceDto(inv);
    }

    @Transactional(readOnly = true)
    public Page<BillDto> listBills(Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return billRepository.findByOrganizationIdOrderByBillDateDescCreatedAtDesc(orgId, pageable).map(this::toBillDto);
    }

    @Transactional
    public BillDto createBill(ZeniusUserPrincipal principal, CreateBillRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultChartOfAccounts(orgId);

        BigDecimal subtotal = req.subtotal();
        BigDecimal tdsRate = req.tdsRate() != null ? req.tdsRate() : BigDecimal.ZERO;
        BigDecimal tdsAmount = subtotal.multiply(tdsRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal netPayable = subtotal.subtract(tdsAmount);

        long seq = billRepository.countByOrganizationId(orgId) + 1;
        FinBillEntity bill = new FinBillEntity();
        bill.setOrganizationId(orgId);
        bill.setBillNumber(String.format("BILL-%05d", seq));
        bill.setVendorName(req.vendorName().trim());
        bill.setBillDate(req.billDate() != null ? req.billDate() : LocalDate.now());
        bill.setDueDate(req.dueDate() != null ? req.dueDate() : LocalDate.now().plusDays(30));
        bill.setSubtotal(subtotal);
        bill.setTdsRate(tdsRate);
        bill.setTdsAmount(tdsAmount);
        bill.setTotalAmount(netPayable);
        bill.setBalanceDue(netPayable);
        bill.setStatus("OPEN");
        bill = billRepository.save(bill);

        UUID expAccount = requireSystemAccountId(orgId, "5100");
        UUID apAccount = requireSystemAccountId(orgId, "2000");
        UUID tdsAccount = requireSystemAccountId(orgId, "2200");
        List<JournalLineInput> lines = new ArrayList<>();
        lines.add(new JournalLineInput(expAccount, "Vendor Expense " + bill.getBillNumber(), subtotal, BigDecimal.ZERO));
        lines.add(new JournalLineInput(apAccount, "AP " + bill.getBillNumber(), BigDecimal.ZERO, netPayable));
        if (tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new JournalLineInput(tdsAccount, "TDS Deducted " + bill.getBillNumber(), BigDecimal.ZERO, tdsAmount));
        }
        postJournal(principal, new CreateJournalRequest(bill.getBillDate(), bill.getBillNumber(), "Vendor Bill " + bill.getBillNumber(), lines));

        return toBillDto(bill);
    }

    @Transactional
    public BillDto payBill(ZeniusUserPrincipal principal, UUID billId, RecordPaymentRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultChartOfAccounts(orgId);
        FinBillEntity bill = billRepository.findByIdAndOrganizationId(billId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (req.amount().compareTo(BigDecimal.ZERO) <= 0 || req.amount().compareTo(bill.getBalanceDue()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining bill balance");
        }

        FinPaymentEntity payment = new FinPaymentEntity();
        payment.setOrganizationId(orgId);
        payment.setPaymentType("PAYABLE");
        payment.setReferenceId(bill.getId());
        payment.setPaymentDate(req.paymentDate() != null ? req.paymentDate() : LocalDate.now());
        payment.setAmount(req.amount());
        payment.setMethod("BANK_TRANSFER");
        paymentRepository.save(payment);

        BigDecimal remaining = bill.getBalanceDue().subtract(req.amount());
        bill.setBalanceDue(remaining);
        bill.setStatus(remaining.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIAL");
        bill = billRepository.save(bill);

        UUID apAccount = requireSystemAccountId(orgId, "2000");
        UUID cashAccount = requireSystemAccountId(orgId, "1000");
        postJournal(principal, new CreateJournalRequest(
                payment.getPaymentDate(),
                bill.getBillNumber(),
                "Vendor payment for " + bill.getBillNumber(),
                List.of(
                        new JournalLineInput(apAccount, "Settle AP", req.amount(), BigDecimal.ZERO),
                        new JournalLineInput(cashAccount, "Bank disbursement", BigDecimal.ZERO, req.amount())
                )
        ));

        return toBillDto(bill);
    }

    @Transactional
    public FinancialReportsDto getFinancialReports() {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultChartOfAccounts(orgId);

        List<FinAccountEntity> accounts = accountRepository.findByOrganizationIdOrderByCodeAsc(orgId);
        List<FinJournalLineEntity> lines = journalLineRepository.findByOrganizationId(orgId);

        Map<UUID, BigDecimal> debits = new HashMap<>();
        Map<UUID, BigDecimal> credits = new HashMap<>();
        for (FinJournalLineEntity l : lines) {
            debits.merge(l.getAccountId(), l.getDebitAmount(), BigDecimal::add);
            credits.merge(l.getAccountId(), l.getCreditAmount(), BigDecimal::add);
        }

        List<TrialBalanceRow> trialBalance = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (FinAccountEntity acc : accounts) {
            BigDecimal dr = debits.getOrDefault(acc.getId(), BigDecimal.ZERO);
            BigDecimal cr = credits.getOrDefault(acc.getId(), BigDecimal.ZERO);
            totalDebits = totalDebits.add(dr);
            totalCredits = totalCredits.add(cr);

            BigDecimal net = switch (acc.getAccountType()) {
                case "ASSET", "EXPENSE" -> dr.subtract(cr);
                default -> cr.subtract(dr);
            };
            if ("REVENUE".equals(acc.getAccountType())) totalRevenue = totalRevenue.add(net);
            if ("EXPENSE".equals(acc.getAccountType())) totalExpense = totalExpense.add(net);
            if ("ASSET".equals(acc.getAccountType())) totalAssets = totalAssets.add(net);
            if ("LIABILITY".equals(acc.getAccountType())) totalLiabilities = totalLiabilities.add(net);

            trialBalance.add(new TrialBalanceRow(acc.getId(), acc.getCode(), acc.getName(), acc.getAccountType(), dr, cr, net));
        }

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);
        BigDecimal arOutstanding = invoiceRepository.findByOrganizationId(orgId).stream()
                .map(FinInvoiceEntity::getBalanceDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal apOutstanding = billRepository.findByOrganizationId(orgId).stream()
                .map(FinBillEntity::getBalanceDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGstCollected = invoiceRepository.findByOrganizationId(orgId).stream()
                .map(FinInvoiceEntity::getGstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTdsDeducted = billRepository.findByOrganizationId(orgId).stream()
                .map(FinBillEntity::getTdsAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinancialReportsDto(
                totalDebits,
                totalCredits,
                totalRevenue,
                totalExpense,
                netProfit,
                totalAssets,
                totalLiabilities,
                arOutstanding,
                apOutstanding,
                totalGstCollected,
                totalTdsDeducted,
                trialBalance
        );
    }

    private void ensureDefaultChartOfAccounts(UUID orgId) {
        if (!accountRepository.findByOrganizationIdOrderByCodeAsc(orgId).isEmpty()) {
            return;
        }
        createSysAcc(orgId, "1000", "Cash & Bank", "ASSET");
        createSysAcc(orgId, "1100", "Accounts Receivable", "ASSET");
        createSysAcc(orgId, "1200", "Inventory Asset", "ASSET");
        createSysAcc(orgId, "2000", "Accounts Payable", "LIABILITY");
        createSysAcc(orgId, "2100", "GST Output Payable", "LIABILITY");
        createSysAcc(orgId, "2200", "TDS Payable", "LIABILITY");
        createSysAcc(orgId, "3000", "Owner Equity", "EQUITY");
        createSysAcc(orgId, "4000", "Operating Revenue", "REVENUE");
        createSysAcc(orgId, "5000", "Cost of Goods Sold", "EXPENSE");
        createSysAcc(orgId, "5100", "Operating Expenses", "EXPENSE");
    }

    private void createSysAcc(UUID orgId, String code, String name, String type) {
        FinAccountEntity a = new FinAccountEntity();
        a.setOrganizationId(orgId);
        a.setCode(code);
        a.setName(name);
        a.setAccountType(type);
        a.setCurrency("USD");
        a.setSystemAccount(true);
        a.setActive(true);
        accountRepository.save(a);
    }

    private UUID requireSystemAccountId(UUID orgId, String code) {
        return accountRepository.findByOrganizationIdAndCodeIgnoreCase(orgId, code)
                .map(FinAccountEntity::getId)
                .orElseThrow(() -> new IllegalStateException("System account missing: " + code));
    }

    private InvoiceDto toInvoiceDto(FinInvoiceEntity i) {
        return new InvoiceDto(i.getId(), i.getInvoiceNumber(), i.getCustomerName(), i.getCustomerEmail(), i.getCurrency(), i.getIssueDate(), i.getDueDate(), i.getSubtotal(), i.getGstRate(), i.getGstAmount(), i.getTdsRate(), i.getTdsAmount(), i.getTotalAmount(), i.getBalanceDue(), i.getStatus(), i.getCreatedAt());
    }

    private BillDto toBillDto(FinBillEntity b) {
        return new BillDto(b.getId(), b.getBillNumber(), b.getVendorName(), b.getBillDate(), b.getDueDate(), b.getSubtotal(), b.getTdsRate(), b.getTdsAmount(), b.getTotalAmount(), b.getBalanceDue(), b.getStatus(), b.getCreatedAt());
    }

    public record AccountDto(UUID id, String code, String name, String accountType, String currency, boolean systemAccount, boolean active) {
    }

    public record CreateAccountRequest(String code, String name, String accountType, String currency) {
    }

    public record JournalLineInput(UUID accountId, String description, BigDecimal debitAmount, BigDecimal creditAmount) {
    }

    public record CreateJournalRequest(LocalDate entryDate, String reference, String description, List<JournalLineInput> lines) {
    }

    public record JournalLineDto(UUID id, UUID accountId, String description, BigDecimal debitAmount, BigDecimal creditAmount) {
    }

    public record JournalEntryDto(UUID id, String entryNumber, LocalDate entryDate, String reference, String description, String status, BigDecimal totalDebit, BigDecimal totalCredit, List<JournalLineDto> lines) {
    }

    public record CreateInvoiceRequest(String customerName, String customerEmail, String currency, LocalDate issueDate, LocalDate dueDate, BigDecimal subtotal, BigDecimal gstRate, BigDecimal tdsRate, String notes) {
    }

    public record InvoiceDto(UUID id, String invoiceNumber, String customerName, String customerEmail, String currency, LocalDate issueDate, LocalDate dueDate, BigDecimal subtotal, BigDecimal gstRate, BigDecimal gstAmount, BigDecimal tdsRate, BigDecimal tdsAmount, BigDecimal totalAmount, BigDecimal balanceDue, String status, Instant createdAt) {
    }

    public record CreateBillRequest(String vendorName, LocalDate billDate, LocalDate dueDate, BigDecimal subtotal, BigDecimal tdsRate) {
    }

    public record BillDto(UUID id, String billNumber, String vendorName, LocalDate billDate, LocalDate dueDate, BigDecimal subtotal, BigDecimal tdsRate, BigDecimal tdsAmount, BigDecimal totalAmount, BigDecimal balanceDue, String status, Instant createdAt) {
    }

    public record RecordPaymentRequest(BigDecimal amount, LocalDate paymentDate, String method, String referenceNote) {
    }

    public record TrialBalanceRow(UUID accountId, String code, String name, String accountType, BigDecimal debit, BigDecimal credit, BigDecimal netBalance) {
    }

    public record FinancialReportsDto(BigDecimal totalDebits, BigDecimal totalCredits, BigDecimal totalRevenue, BigDecimal totalExpense, BigDecimal netProfit, BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal arOutstanding, BigDecimal apOutstanding, BigDecimal totalGstCollected, BigDecimal totalTdsDeducted, List<TrialBalanceRow> trialBalance) {
    }
}

