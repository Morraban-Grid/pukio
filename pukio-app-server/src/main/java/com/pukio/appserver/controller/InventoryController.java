package com.pukio.appserver.controller;

import com.pukio.appserver.business.InventoryService;
import com.pukio.appserver.domain.Inventory;
import com.pukio.appserver.dto.InventoryAdjustmentRequest;
import com.pukio.appserver.dto.InventoryTransferRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{sku}")
    public ResponseEntity<Integer> getInventory(
            @PathVariable String sku,
            @RequestParam String storeId) {
        int stock = inventoryService.checkStock(sku, storeId);
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/adjustment")
    public ResponseEntity<Inventory> adjustInventory(@Valid @RequestBody InventoryAdjustmentRequest request) {
        Inventory result = inventoryService.adjustInventory(
                request.getSku(), request.getStoreId(), request.getDelta(),
                request.getReason(), null, null);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferStock(@Valid @RequestBody InventoryTransferRequest request) {
        inventoryService.transferStock(
                request.getSku(), request.getFromStoreId(), request.getToStoreId(),
                request.getQuantity(), null, null);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<Inventory>> getInventoryByStore(@PathVariable String storeId) {
        List<Inventory> inventory = inventoryService.getInventoryByStore(storeId);
        return ResponseEntity.ok(inventory);
    }
}
