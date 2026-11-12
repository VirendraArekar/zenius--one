package com.zenius.one.modules.inventory;

import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InvWarehouseRepository warehouseRepository;
    private final InvItemRepository itemRepository;
    private final InvStockLevelRepository stockLevelRepository;
    private final InvStockMovementRepository movementRepository;
    private final InvPurchaseOrderRepository poRepository;
    private final InvStoreOrderRepository storeOrderRepository;

    public InventoryService(
            InvWarehouseRepository warehouseRepository,
            InvItemRepository itemRepository,
            InvStockLevelRepository stockLevelRepository,
            InvStockMovementRepository movementRepository,
            InvPurchaseOrderRepository poRepository,
            InvStoreOrderRepository storeOrderRepository
    ) {
        this.warehouseRepository = warehouseRepository;
        this.itemRepository = itemRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.movementRepository = movementRepository;
        this.poRepository = poRepository;
        this.storeOrderRepository = storeOrderRepository;
    }

    @Transactional
    public List<WarehouseDto> listWarehouses() {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultWarehouse(orgId);
        return warehouseRepository.findByOrganizationIdOrderByDefaultWarehouseDescNameAsc(orgId).stream()
                .map(w -> new WarehouseDto(w.getId(), w.getCode(), w.getName(), w.getCity(), w.isDefaultWarehouse()))
                .toList();
    }

    @Transactional
    public WarehouseDto createWarehouse(CreateWarehouseRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultWarehouse(orgId);
        String code = req.code().trim().toUpperCase();
        if (warehouseRepository.existsByOrganizationIdAndCodeIgnoreCase(orgId, code)) {
            throw new IllegalStateException("Warehouse code already exists: " + code);
        }
        InvWarehouseEntity w = new InvWarehouseEntity();
        w.setOrganizationId(orgId);
        w.setCode(code);
        w.setName(req.name().trim());
        w.setCity(req.city());
        w.setDefaultWarehouse(false);
        w = warehouseRepository.save(w);
        return new WarehouseDto(w.getId(), w.getCode(), w.getName(), w.getCity(), w.isDefaultWarehouse());
    }

    @Transactional
    public List<ItemWithStockDto> listItems() {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultWarehouse(orgId);
        Map<UUID, Integer> totalByItem = stockLevelRepository.findByOrganizationId(orgId).stream()
                .collect(Collectors.groupingBy(InvStockLevelEntity::getItemId,
                        Collectors.summingInt(InvStockLevelEntity::getQuantityOnHand)));
        return itemRepository.findByOrganizationIdOrderByNameAsc(orgId).stream()
                .map(i -> {
                    int qty = totalByItem.getOrDefault(i.getId(), 0);
                    return new ItemWithStockDto(i.getId(), i.getSku(), i.getName(), i.getCategory(), i.getUnitCost(), i.getSellingPrice(), i.getReorderLevel(), qty, qty <= i.getReorderLevel());
                })
                .toList();
    }

    @Transactional
    public ItemWithStockDto createItem(CreateItemRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        InvWarehouseEntity defaultWh = ensureDefaultWarehouse(orgId);
        String sku = req.sku().trim().toUpperCase();
        if (itemRepository.existsByOrganizationIdAndSkuIgnoreCase(orgId, sku)) {
            throw new IllegalStateException("Inventory SKU already exists: " + sku);
        }
        InvItemEntity item = new InvItemEntity();
        item.setOrganizationId(orgId);
        item.setSku(sku);
        item.setName(req.name().trim());
        item.setCategory(req.category());
        item.setUnitCost(req.unitCost());
        item.setSellingPrice(req.sellingPrice());
        item.setReorderLevel(req.reorderLevel() != null ? req.reorderLevel() : 10);
        item.setTrackSerial(req.trackSerial());
        item = itemRepository.save(item);

        int initialQty = req.initialQuantity() != null && req.initialQuantity() > 0 ? req.initialQuantity() : 0;
        if (initialQty > 0) {
            adjustStockInternal(orgId, item.getId(), defaultWh.getId(), initialQty, "INITIAL_STOCK", "Initial inventory opening balance");
        }
        return new ItemWithStockDto(item.getId(), item.getSku(), item.getName(), item.getCategory(), item.getUnitCost(), item.getSellingPrice(), item.getReorderLevel(), initialQty, initialQty <= item.getReorderLevel());
    }

    @Transactional
    public StockLevelDto adjustStock(AdjustStockRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        itemRepository.findByIdAndOrganizationId(req.itemId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in current organization"));
        warehouseRepository.findByIdAndOrganizationId(req.warehouseId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found in current organization"));
        InvStockLevelEntity level = adjustStockInternal(
                orgId, req.itemId(), req.warehouseId(), req.quantityDelta(), "ADJUSTMENT", req.reason()
        );
        return new StockLevelDto(level.getItemId(), level.getWarehouseId(), level.getQuantityOnHand());
    }

    @Transactional
    public TransferResult transferStock(TransferStockRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (req.quantity() <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }
        if (req.fromWarehouseId().equals(req.toWarehouseId())) {
            throw new IllegalArgumentException("Source and destination warehouses must differ");
        }
        itemRepository.findByIdAndOrganizationId(req.itemId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in current organization"));
        warehouseRepository.findByIdAndOrganizationId(req.fromWarehouseId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Source warehouse not found"));
        warehouseRepository.findByIdAndOrganizationId(req.toWarehouseId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Destination warehouse not found"));

        InvStockLevelEntity from = adjustStockInternal(orgId, req.itemId(), req.fromWarehouseId(), -req.quantity(), "TRANSFER_OUT", "Transfer out");
        InvStockLevelEntity to = adjustStockInternal(orgId, req.itemId(), req.toWarehouseId(), req.quantity(), "TRANSFER_IN", "Transfer in");
        return new TransferResult(from.getQuantityOnHand(), to.getQuantityOnHand());
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> listPurchaseOrders() {
        UUID orgId = TenantContext.requireOrganizationId();
        return poRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(this::toPoDto)
                .toList();
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePoRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        InvItemEntity item = itemRepository.findByIdAndOrganizationId(req.itemId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        warehouseRepository.findByIdAndOrganizationId(req.warehouseId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        long seq = poRepository.countByOrganizationId(orgId) + 1;
        InvPurchaseOrderEntity po = new InvPurchaseOrderEntity();
        po.setOrganizationId(orgId);
        po.setPoNumber(String.format("PO-%05d", seq));
        po.setVendorName(req.vendorName().trim());
        po.setWarehouseId(req.warehouseId());
        po.setItemId(item.getId());
        po.setQuantity(req.quantity());
        po.setUnitCost(req.unitCost());
        po.setTotalCost(req.unitCost().multiply(BigDecimal.valueOf(req.quantity())));
        po.setStatus("ORDERED");
        return toPoDto(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDto receivePurchaseOrder(UUID poId) {
        UUID orgId = TenantContext.requireOrganizationId();
        InvPurchaseOrderEntity po = poRepository.findByIdAndOrganizationId(poId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + poId));
        if ("RECEIVED".equals(po.getStatus())) {
            throw new IllegalStateException("Purchase order already received");
        }
        po.setStatus("RECEIVED");
        po = poRepository.save(po);
        adjustStockInternal(orgId, po.getItemId(), po.getWarehouseId(), po.getQuantity(), "PO_RECEIPT", "Received " + po.getPoNumber());
        return toPoDto(po);
    }

    @Transactional(readOnly = true)
    public List<StoreOrderDto> listStoreOrders() {
        UUID orgId = TenantContext.requireOrganizationId();
        return storeOrderRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(o -> new StoreOrderDto(o.getId(), o.getOrderNumber(), o.getCustomerName(), o.getCustomerEmail(), o.getWarehouseId(), o.getItemId(), o.getQuantity(), o.getTotalAmount(), o.getStatus()))
                .toList();
    }

    @Transactional
    public StoreOrderDto checkoutStoreOrder(CheckoutStoreOrderRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        InvItemEntity item = itemRepository.findByIdAndOrganizationId(req.itemId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        InvWarehouseEntity wh = req.warehouseId() != null
                ? warehouseRepository.findByIdAndOrganizationId(req.warehouseId(), orgId)
                        .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"))
                : ensureDefaultWarehouse(orgId);

        adjustStockInternal(orgId, item.getId(), wh.getId(), -req.quantity(), "STORE_SALE", "E-commerce checkout");

        long seq = storeOrderRepository.countByOrganizationId(orgId) + 1;
        InvStoreOrderEntity order = new InvStoreOrderEntity();
        order.setOrganizationId(orgId);
        order.setOrderNumber(String.format("ORD-%05d", seq));
        order.setCustomerName(req.customerName().trim());
        order.setCustomerEmail(req.customerEmail().trim().toLowerCase());
        order.setWarehouseId(wh.getId());
        order.setItemId(item.getId());
        order.setQuantity(req.quantity());
        order.setTotalAmount(item.getSellingPrice().multiply(BigDecimal.valueOf(req.quantity())));
        order.setStatus("PAID");
        order = storeOrderRepository.save(order);
        return new StoreOrderDto(order.getId(), order.getOrderNumber(), order.getCustomerName(), order.getCustomerEmail(), order.getWarehouseId(), order.getItemId(), order.getQuantity(), order.getTotalAmount(), order.getStatus());
    }

    private InvStockLevelEntity adjustStockInternal(
            UUID orgId,
            UUID itemId,
            UUID warehouseId,
            int delta,
            String type,
            String note
    ) {
        InvStockLevelEntity level = stockLevelRepository
                .findByOrganizationIdAndItemIdAndWarehouseId(orgId, itemId, warehouseId)
                .orElseGet(() -> {
                    InvStockLevelEntity created = new InvStockLevelEntity();
                    created.setOrganizationId(orgId);
                    created.setItemId(itemId);
                    created.setWarehouseId(warehouseId);
                    created.setQuantityOnHand(0);
                    return created;
                });

        int nextQty = level.getQuantityOnHand() + delta;
        if (nextQty < 0) {
            throw new IllegalStateException("Insufficient stock in warehouse (available: " + level.getQuantityOnHand() + ")");
        }
        level.setQuantityOnHand(nextQty);
        level = stockLevelRepository.save(level);

        InvStockMovementEntity mv = new InvStockMovementEntity();
        mv.setOrganizationId(orgId);
        mv.setItemId(itemId);
        mv.setWarehouseId(warehouseId);
        mv.setMovementType(type);
        mv.setQuantityDelta(delta);
        mv.setReferenceNote(note);
        movementRepository.save(mv);
        return level;
    }

    private InvWarehouseEntity ensureDefaultWarehouse(UUID orgId) {
        List<InvWarehouseEntity> existing = warehouseRepository.findByOrganizationIdOrderByDefaultWarehouseDescNameAsc(orgId);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        InvWarehouseEntity main = new InvWarehouseEntity();
        main.setOrganizationId(orgId);
        main.setCode("MAIN");
        main.setName("Main Fulfillment Warehouse");
        main.setDefaultWarehouse(true);
        return warehouseRepository.save(main);
    }

    private PurchaseOrderDto toPoDto(InvPurchaseOrderEntity p) {
        return new PurchaseOrderDto(p.getId(), p.getPoNumber(), p.getVendorName(), p.getWarehouseId(), p.getItemId(), p.getQuantity(), p.getUnitCost(), p.getTotalCost(), p.getStatus());
    }

    public record WarehouseDto(UUID id, String code, String name, String city, boolean defaultWarehouse) {
    }

    public record CreateWarehouseRequest(String code, String name, String city) {
    }

    public record CreateItemRequest(String sku, String name, String category, BigDecimal unitCost, BigDecimal sellingPrice, Integer reorderLevel, boolean trackSerial, Integer initialQuantity) {
    }

    public record ItemWithStockDto(UUID id, String sku, String name, String category, BigDecimal unitCost, BigDecimal sellingPrice, Integer reorderLevel, int totalQuantityOnHand, boolean lowStock) {
    }

    public record AdjustStockRequest(UUID itemId, UUID warehouseId, int quantityDelta, String reason) {
    }

    public record StockLevelDto(UUID itemId, UUID warehouseId, int quantityOnHand) {
    }

    public record TransferStockRequest(UUID itemId, UUID fromWarehouseId, UUID toWarehouseId, int quantity) {
    }

    public record TransferResult(int fromWarehouseQuantity, int toWarehouseQuantity) {
    }

    public record CreatePoRequest(String vendorName, UUID warehouseId, UUID itemId, int quantity, BigDecimal unitCost) {
    }

    public record PurchaseOrderDto(UUID id, String poNumber, String vendorName, UUID warehouseId, UUID itemId, Integer quantity, BigDecimal unitCost, BigDecimal totalCost, String status) {
    }

    public record CheckoutStoreOrderRequest(String customerName, String customerEmail, UUID warehouseId, UUID itemId, int quantity) {
    }

    public record StoreOrderDto(UUID id, String orderNumber, String customerName, String customerEmail, UUID warehouseId, UUID itemId, Integer quantity, BigDecimal totalAmount, String status) {
    }
}

