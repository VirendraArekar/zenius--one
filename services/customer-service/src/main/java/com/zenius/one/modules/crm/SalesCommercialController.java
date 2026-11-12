package com.zenius.one.modules.crm;

import com.zenius.one.modules.crm.CrmCommercialService.ActivityDto;
import com.zenius.one.modules.crm.CrmCommercialService.CreateActivityRequest;
import com.zenius.one.modules.crm.CrmCommercialService.CreateProductRequest;
import com.zenius.one.modules.crm.CrmCommercialService.CreateQuoteRequest;
import com.zenius.one.modules.crm.CrmCommercialService.ProductDto;
import com.zenius.one.modules.crm.CrmCommercialService.QuoteDto;
import com.zenius.one.modules.crm.CrmCommercialService.QuoteLineInput;
import com.zenius.one.modules.crm.CrmCommercialService.SalesOrderDto;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crm")
@Validated
@Tag(name = "Sales Commercials", description = "Products, Quotes, Sales Orders, and CRM Activities")
public class SalesCommercialController {

    private final CrmCommercialService commercialService;

    public SalesCommercialController(CrmCommercialService commercialService) {
        this.commercialService = commercialService;
    }

    @GetMapping("/products")
    @PreAuthorize("@authz.hasPermission('crm:product:read')")
    public ApiResponse<List<ProductDto>> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        Page<ProductDto> res = commercialService.listProducts(q, PageRequest.of(page, size, Sort.by("name").ascending()));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('crm:product:write')")
    public ApiResponse<ProductDto> createProduct(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody ProductBody body
    ) {
        return ApiResponse.ok(commercialService.createProduct(
                principal,
                new CreateProductRequest(body.sku(), body.name(), body.description(), body.category(), body.unitPrice(), body.taxRate())
        ));
    }

    @GetMapping("/quotes")
    @PreAuthorize("@authz.hasPermission('crm:quote:read')")
    public ApiResponse<List<QuoteDto>> listQuotes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<QuoteDto> res = commercialService.listQuotes(status, q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('crm:quote:write')")
    public ApiResponse<QuoteDto> createQuote(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody QuoteBody body
    ) {
        List<QuoteLineInput> lines = body.lines().stream()
                .map(l -> new QuoteLineInput(l.productId(), l.itemName(), l.quantity(), l.unitPrice(), l.discountPercent(), l.taxRate()))
                .toList();
        return ApiResponse.ok(commercialService.createQuote(
                principal,
                new CreateQuoteRequest(body.accountId(), body.contactId(), body.dealId(), body.subject(), body.validUntil(), body.notes(), lines)
        ));
    }

    @PatchMapping("/quotes/{id}/status")
    @PreAuthorize("@authz.hasPermission('crm:quote:write')")
    public ApiResponse<QuoteDto> updateQuoteStatus(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody QuoteStatusBody body
    ) {
        return ApiResponse.ok(commercialService.updateQuoteStatus(principal, id, body.status()));
    }

    @PostMapping("/quotes/{id}/convert-to-order")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('crm:quote:write')")
    public ApiResponse<SalesOrderDto> convertQuoteToOrder(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(commercialService.convertQuoteToOrder(principal, id));
    }

    @GetMapping("/sales-orders")
    @PreAuthorize("@authz.hasPermission('crm:quote:read')")
    public ApiResponse<List<SalesOrderDto>> listSalesOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<SalesOrderDto> res = commercialService.listSalesOrders(PageRequest.of(page, size));
        return ApiResponse.page(res, res.getContent());
    }

    @GetMapping("/activities")
    @PreAuthorize("@authz.hasPermission('crm:activity:read')")
    public ApiResponse<List<ActivityDto>> listActivities(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        Page<ActivityDto> res = commercialService.listActivities(entityType, entityId, PageRequest.of(page, size));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/activities")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('crm:activity:write')")
    public ApiResponse<ActivityDto> createActivity(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody ActivityBody body
    ) {
        return ApiResponse.ok(commercialService.createActivity(
                principal,
                new CreateActivityRequest(
                        body.activityType(), body.subject(), body.description(),
                        body.relatedEntityType(), body.relatedEntityId(), body.dueAt()
                )
        ));
    }

    @PostMapping("/activities/{id}/complete")
    @PreAuthorize("@authz.hasPermission('crm:activity:write')")
    public ApiResponse<ActivityDto> completeActivity(@PathVariable UUID id) {
        return ApiResponse.ok(commercialService.completeActivity(id));
    }

    public record ProductBody(
            @NotBlank String sku,
            @NotBlank String name,
            String description,
            String category,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            BigDecimal taxRate
    ) {
    }

    public record QuoteLineBody(
            UUID productId,
            @NotBlank String itemName,
            @NotNull @DecimalMin("0.01") BigDecimal quantity,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal taxRate
    ) {
    }

    public record QuoteBody(
            UUID accountId,
            UUID contactId,
            UUID dealId,
            @NotBlank String subject,
            LocalDate validUntil,
            String notes,
            @NotEmpty @Valid List<QuoteLineBody> lines
    ) {
    }

    public record QuoteStatusBody(@NotBlank String status) {
    }

    public record ActivityBody(
            @NotBlank String activityType,
            @NotBlank String subject,
            String description,
            @NotBlank String relatedEntityType,
            @NotNull UUID relatedEntityId,
            Instant dueAt
    ) {
    }
}

