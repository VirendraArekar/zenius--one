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
public class CrmCommercialService {

    private final CrmProductRepository productRepository;
    private final CrmQuoteRepository quoteRepository;
    private final CrmQuoteLineRepository quoteLineRepository;
    private final CrmSalesOrderRepository salesOrderRepository;
    private final CrmActivityRepository activityRepository;
    private final AuditLogService auditLogService;

    public CrmCommercialService(
            CrmProductRepository productRepository,
            CrmQuoteRepository quoteRepository,
            CrmQuoteLineRepository quoteLineRepository,
            CrmSalesOrderRepository salesOrderRepository,
            CrmActivityRepository activityRepository,
            AuditLogService auditLogService
    ) {
        this.productRepository = productRepository;
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.activityRepository = activityRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> listProducts(String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        String normQ = (query == null || query.isBlank()) ? null : query.trim();
        return productRepository.search(orgId, normQ, pageable).map(this::toProductDto);
    }

    @Transactional
    public ProductDto createProduct(ZeniusUserPrincipal principal, CreateProductRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String sku = req.sku().trim().toUpperCase();
        if (productRepository.existsByOrganizationIdAndSkuIgnoreCase(orgId, sku)) {
            throw new IllegalStateException("Product SKU already exists: " + sku);
        }
        CrmProductEntity product = new CrmProductEntity();
        product.setOrganizationId(orgId);
        product.setSku(sku);
        product.setName(req.name().trim());
        product.setDescription(req.description());
        product.setCategory(req.category());
        product.setUnitPrice(req.unitPrice());
        product.setTaxRate(req.taxRate() != null ? req.taxRate() : BigDecimal.ZERO);
        product.setActive(true);
        product = productRepository.save(product);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_PRODUCT_CREATED",
                "CRM_PRODUCT",
                product.getId().toString(),
                "Created product " + product.getSku() + " - " + product.getName(),
                "SUCCESS"
        );
        return toProductDto(product);
    }

    @Transactional(readOnly = true)
    public Page<QuoteDto> listQuotes(String status, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        String normStatus = (status == null || status.isBlank()) ? null : status.trim();
        String normQ = (query == null || query.isBlank()) ? null : query.trim();
        return quoteRepository.search(orgId, normStatus, normQ, pageable).map(q -> {
            List<QuoteLineDto> lines = quoteLineRepository
                    .findByOrganizationIdAndQuoteIdOrderByCreatedAtAsc(orgId, q.getId())
                    .stream()
                    .map(this::toQuoteLineDto)
                    .toList();
            return toQuoteDto(q, lines);
        });
    }

    @Transactional
    public QuoteDto createQuote(ZeniusUserPrincipal principal, CreateQuoteRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        long nextSeq = quoteRepository.countByOrganizationId(orgId) + 1;

        CrmQuoteEntity quote = new CrmQuoteEntity();
        quote.setOrganizationId(orgId);
        quote.setQuoteNumber(String.format("QT-%05d", nextSeq));
        quote.setAccountId(req.accountId());
        quote.setContactId(req.contactId());
        quote.setDealId(req.dealId());
        quote.setOwnerUserId(principal.getUserId());
        quote.setSubject(req.subject().trim());
        quote.setStatus("DRAFT");
        quote.setValidUntil(req.validUntil() != null ? req.validUntil() : LocalDate.now().plusDays(15));
        quote.setNotes(req.notes());
        quote = quoteRepository.save(quote);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        List<QuoteLineDto> lineDtos = new ArrayList<>();

        for (QuoteLineInput lineIn : req.lines()) {
            BigDecimal gross = lineIn.unitPrice().multiply(lineIn.quantity());
            BigDecimal discountPct = lineIn.discountPercent() != null ? lineIn.discountPercent() : BigDecimal.ZERO;
            BigDecimal discountAmt = gross.multiply(discountPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal net = gross.subtract(discountAmt);
            BigDecimal taxPct = lineIn.taxRate() != null ? lineIn.taxRate() : BigDecimal.ZERO;
            BigDecimal taxAmt = net.multiply(taxPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            CrmQuoteLineEntity line = new CrmQuoteLineEntity();
            line.setOrganizationId(orgId);
            line.setQuoteId(quote.getId());
            line.setProductId(lineIn.productId());
            line.setItemName(lineIn.itemName().trim());
            line.setQuantity(lineIn.quantity());
            line.setUnitPrice(lineIn.unitPrice());
            line.setDiscountPercent(discountPct);
            line.setTaxRate(taxPct);
            line.setLineTotal(net.add(taxAmt));
            line = quoteLineRepository.save(line);

            subtotal = subtotal.add(net);
            taxTotal = taxTotal.add(taxAmt);
            lineDtos.add(toQuoteLineDto(line));
        }

        quote.setSubtotal(subtotal);
        quote.setTaxTotal(taxTotal);
        quote.setGrandTotal(subtotal.add(taxTotal));
        quote = quoteRepository.save(quote);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_QUOTE_CREATED",
                "CRM_QUOTE",
                quote.getId().toString(),
                "Created quote " + quote.getQuoteNumber() + " total " + quote.getGrandTotal(),
                "SUCCESS"
        );
        return toQuoteDto(quote, lineDtos);
    }

    @Transactional
    public QuoteDto updateQuoteStatus(ZeniusUserPrincipal principal, UUID quoteId, String status) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmQuoteEntity quote = quoteRepository.findByIdAndOrganizationId(quoteId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + quoteId));
        quote.setStatus(status.trim().toUpperCase());
        quote = quoteRepository.save(quote);
        List<QuoteLineDto> lines = quoteLineRepository
                .findByOrganizationIdAndQuoteIdOrderByCreatedAtAsc(orgId, quote.getId())
                .stream()
                .map(this::toQuoteLineDto)
                .toList();
        return toQuoteDto(quote, lines);
    }

    @Transactional
    public SalesOrderDto convertQuoteToOrder(ZeniusUserPrincipal principal, UUID quoteId) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmQuoteEntity quote = quoteRepository.findByIdAndOrganizationId(quoteId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + quoteId));

        quote.setStatus("ACCEPTED");
        quoteRepository.save(quote);

        long nextSeq = salesOrderRepository.countByOrganizationId(orgId) + 1;
        CrmSalesOrderEntity order = new CrmSalesOrderEntity();
        order.setOrganizationId(orgId);
        order.setOrderNumber(String.format("SO-%05d", nextSeq));
        order.setQuoteId(quote.getId());
        order.setAccountId(quote.getAccountId());
        order.setDealId(quote.getDealId());
        order.setSubject(quote.getSubject());
        order.setStatus("CONFIRMED");
        order.setOrderDate(LocalDate.now());
        order.setGrandTotal(quote.getGrandTotal());
        order = salesOrderRepository.save(order);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CRM_SALES_ORDER_CREATED",
                "CRM_SALES_ORDER",
                order.getId().toString(),
                "Converted quote " + quote.getQuoteNumber() + " to sales order " + order.getOrderNumber(),
                "SUCCESS"
        );
        return toOrderDto(order);
    }

    @Transactional(readOnly = true)
    public Page<SalesOrderDto> listSalesOrders(Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return salesOrderRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable).map(this::toOrderDto);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDto> listActivities(String entityType, UUID entityId, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        String normType = (entityType == null || entityType.isBlank()) ? null : entityType.trim();
        return activityRepository.search(orgId, normType, entityId, pageable).map(this::toActivityDto);
    }

    @Transactional
    public ActivityDto createActivity(ZeniusUserPrincipal principal, CreateActivityRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmActivityEntity activity = new CrmActivityEntity();
        activity.setOrganizationId(orgId);
        activity.setOwnerUserId(principal.getUserId());
        activity.setActivityType(req.activityType().trim().toUpperCase());
        activity.setSubject(req.subject().trim());
        activity.setDescription(req.description());
        activity.setRelatedEntityType(req.relatedEntityType().trim().toUpperCase());
        activity.setRelatedEntityId(req.relatedEntityId());
        activity.setDueAt(req.dueAt());
        activity.setStatus("OPEN");
        activity = activityRepository.save(activity);
        return toActivityDto(activity);
    }

    @Transactional
    public ActivityDto completeActivity(UUID activityId) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmActivityEntity activity = activityRepository.findByIdAndOrganizationId(activityId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + activityId));
        activity.setStatus("COMPLETED");
        activity.setCompletedAt(Instant.now());
        return toActivityDto(activityRepository.save(activity));
    }

    private ProductDto toProductDto(CrmProductEntity p) {
        return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getCategory(), p.getUnitPrice(), p.getTaxRate(), p.isActive());
    }

    private QuoteLineDto toQuoteLineDto(CrmQuoteLineEntity l) {
        return new QuoteLineDto(l.getId(), l.getProductId(), l.getItemName(), l.getQuantity(), l.getUnitPrice(), l.getDiscountPercent(), l.getTaxRate(), l.getLineTotal());
    }

    private QuoteDto toQuoteDto(CrmQuoteEntity q, List<QuoteLineDto> lines) {
        return new QuoteDto(q.getId(), q.getQuoteNumber(), q.getAccountId(), q.getContactId(), q.getDealId(), q.getSubject(), q.getStatus(), q.getValidUntil(), q.getSubtotal(), q.getTaxTotal(), q.getGrandTotal(), q.getNotes(), lines, q.getCreatedAt());
    }

    private SalesOrderDto toOrderDto(CrmSalesOrderEntity o) {
        return new SalesOrderDto(o.getId(), o.getOrderNumber(), o.getQuoteId(), o.getAccountId(), o.getDealId(), o.getSubject(), o.getStatus(), o.getOrderDate(), o.getGrandTotal(), o.getCreatedAt());
    }

    private ActivityDto toActivityDto(CrmActivityEntity a) {
        return new ActivityDto(a.getId(), a.getActivityType(), a.getSubject(), a.getDescription(), a.getRelatedEntityType(), a.getRelatedEntityId(), a.getDueAt(), a.getCompletedAt(), a.getStatus(), a.getCreatedAt());
    }

    public record CreateProductRequest(String sku, String name, String description, String category, BigDecimal unitPrice, BigDecimal taxRate) {
    }

    public record ProductDto(UUID id, String sku, String name, String description, String category, BigDecimal unitPrice, BigDecimal taxRate, boolean active) {
    }

    public record QuoteLineInput(UUID productId, String itemName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal taxRate) {
    }

    public record CreateQuoteRequest(UUID accountId, UUID contactId, UUID dealId, String subject, LocalDate validUntil, String notes, List<QuoteLineInput> lines) {
    }

    public record QuoteLineDto(UUID id, UUID productId, String itemName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal taxRate, BigDecimal lineTotal) {
    }

    public record QuoteDto(UUID id, String quoteNumber, UUID accountId, UUID contactId, UUID dealId, String subject, String status, LocalDate validUntil, BigDecimal subtotal, BigDecimal taxTotal, BigDecimal grandTotal, String notes, List<QuoteLineDto> lines, Instant createdAt) {
    }

    public record SalesOrderDto(UUID id, String orderNumber, UUID quoteId, UUID accountId, UUID dealId, String subject, String status, LocalDate orderDate, BigDecimal grandTotal, Instant createdAt) {
    }

    public record CreateActivityRequest(String activityType, String subject, String description, String relatedEntityType, UUID relatedEntityId, Instant dueAt) {
    }

    public record ActivityDto(UUID id, String activityType, String subject, String description, String relatedEntityType, UUID relatedEntityId, Instant dueAt, Instant completedAt, String status, Instant createdAt) {
    }
}

