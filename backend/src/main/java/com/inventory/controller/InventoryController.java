package com.inventory.controller;

import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryStatsDTO;
import com.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryDTO>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.findAll());
    }

    @GetMapping("/stats")
    public ResponseEntity<InventoryStatsDTO> stats() {
        return ResponseEntity.ok(inventoryService.stats());
    }

    @GetMapping("/{barcode}")
    public ResponseEntity<InventoryDTO> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(inventoryService.findByBarcode(barcode));
    }

    // Lets a department (or admin) see what's currently available in a category -
    // name, remaining quantity, and specs - before deciding what to request.
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<InventoryDTO>> byCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(inventoryService.findByCategory(categoryId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryDTO> create(@Valid @RequestBody InventoryDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryDTO> update(@PathVariable UUID id, @Valid @RequestBody InventoryDTO dto) {
        return ResponseEntity.ok(inventoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        inventoryService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
