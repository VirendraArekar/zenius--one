package com.zenius.one.modules.inventory;

import com.zenius.one.modules.inventory.InventoryService.AdjustStockRequest;
import com.zenius.one.modules.inventory.InventoryService.CheckoutStoreOrderRequest;
import com.zenius.one.modules.inventory.InventoryService.CreateItemRequest;
import com.zenius.one.modules.inventory.InventoryService.CreatePoRequest;
import com.zenius.one.modules.inventory.InventoryService.CreateWarehouseRequest;
import com.zenius.one.modules.inventory.InventoryService.ItemWithStockDto;
import com.zenius.one.modules.inventory.InventoryService.PurchaseOrderDto;
import com.zenius.one.modules.inventory.InventoryService.StockLevelDto;
import com.zenius.one.modules.inventory.InventoryService.StoreOrderDto;
import com.zenius.one.modules.inventory.InventoryService.TransferResult;
import com.zenius.one.modules.inventory.InventoryService.TransferStockRequest;
import com.zenius.one.modules.inventory.InventoryService.WarehouseDto;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory & Commerce", description = "Multi-warehouse stock, transfers, purchase orders, and online store checkout")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/warehouses")
    @PreAuthorize("@authz.hasPermission('inventory:read')")
    public ApiResponse<List<WarehouseDto>> listWarehouses() {
        return ApiResponse.ok(inventoryService.listWarehouses());
    }

    @PostMapping("/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('inventory:write')")
    public ApiResponse<WarehouseDto> createWarehouse(@Valid @RequestBody WarehouseBody body) {
        return ApiResponse.ok(inventoryService.createWarehouse(new CreateWarehouseRequest(body.code(), body.name(), body.city())));
    }

    @GetMapping("/items")
    @PreAuthorize("@authz.hasPermission('inventory:read')")
    public ApiResponse<List<ItemWithStockDto>> listItems() {
        return ApiResponse.ok(inventoryService.listItems());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('inventory:write')")
    public ApiResponse<ItemWithStockDto> createItem(@Valid @RequestBody ItemBody body) {
        return ApiResponse.ok(inventoryService.createItem(
                new CreateItemRequest(body.sku(), body.name(), body.category(), body.unitCost(), body.sellingPrice(), body.reorderLevel(), body.trackSerial(), body.initialQuantity())
        ));
    }

    @PostMapping("/stock-adjustments")
    @PreAuthorize("@authz.hasPermission('inventory:write')")
    public ApiResponse<StockLevelDto> adjustStock(@Valid @RequestBody AdjustBody body) {
        return ApiResponse.ok(inventoryService.adjustStock(new AdjustStockRequest(body.itemId(), body.warehouseId(), body.quantityDelta(), body.reason())));
    }

    @PostMapping("/transfers")
    @PreAuthorize("@authz.hasPermission('inventory:write')")
    public ApiResponse<TransferResult> transferStock(@Valid @RequestBody TransferBody body) {
        return ApiResponse.ok(inventoryService.transferStock(new TransferStockRequest(body.itemId(), body.fromWarehouseId(), body.toWarehouseId(), body.quantity())));
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("@authz.hasPermission('inventory:read')")
    public ApiResponse<List<PurchaseOrderDto>> listPurchaseOrders() {
        return ApiResponse.ok(inventoryService.listPurchaseOrders());
    }

    @PostMapping("/purchase-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('inventory:write')")
    public ApiResponse<PurchaseOrderDto> createPurchaseOrder(@Valid @RequestBody PoBody body) {
        return ApiResponse.ok(inventoryService.createPurchaseOrder(new CreatePoRequest(body.vendorName(), body.warehouseId(), body.itemId(), body.quantity(), body.unitCost())));
    }

    @PostMapping("/purchase-orders/{id}/receive")
    @PreAuthorize("@authz.hasPermission('inventory:write')")
    public ApiResponse<PurchaseOrderDto> receivePurchaseOrder(@PathVariable UUID id) {
        return ApiResponse.ok(inventoryService.receivePurchaseOrder(id));
    }

    @GetMapping("/store-orders")
    @PreAuthorize("@authz.hasPermission('inventory:read')")
    public ApiResponse<List<StoreOrderDto>> listStoreOrders() {
        return ApiResponse.ok(inventoryService.listStoreOrders());
    }

    @PostMapping("/store-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('inventory:write')")
    public ApiResponse<StoreOrderDto> checkoutStoreOrder(@Valid @RequestBody StoreOrderBody body) {
        return ApiResponse.ok(inventoryService.checkoutStoreOrder(
                new CheckoutStoreOrderRequest(body.customerName(), body.customerEmail(), body.warehouseId(), body.itemId(), body.quantity())
        ));
    }

    public record WarehouseBody(@NotBlank String code, @NotBlank String name, String city) {
    }

    public record ItemBody(
            @NotBlank String sku,
            @NotBlank String name,
            String category,
            @NotNull @DecimalMin("0.00") BigDecimal unitCost,
            @NotNull @DecimalMin("0.00") BigDecimal sellingPrice,
            Integer reorderLevel,
            boolean trackSerial,
            Integer initialQuantity
    ) {
    }

    public record AdjustBody(@NotNull UUID itemId, @NotNull UUID warehouseId, int quantityDelta, String reason) {
    }

    public record TransferBody(@NotNull UUID itemId, @NotNull UUID fromWarehouseId, @NotNull UUID toWarehouseId, @Min(1) int quantity) {
    }

    public record PoBody(@NotBlank String vendorName, @NotNull UUID warehouseId, @NotNull UUID itemId, @Min(1) int quantity, @NotNull @DecimalMin("0.00") BigDecimal unitCost) {
    }

    public record StoreOrderBody(@NotBlank String customerName, @NotBlank @Email String customerEmail, UUID warehouseId, @NotNull UUID itemId, @Min(1) int quantity) {
    }
}

