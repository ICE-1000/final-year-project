package com.inventory.controller;

import com.google.zxing.WriterException;
import com.inventory.barcode.BarcodeGenerator;
import com.inventory.dto.AllocationDTO;
import com.inventory.dto.InventoryDTO;
import com.inventory.dto.ScanResultDTO;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.service.AllocationService;
import com.inventory.service.BarcodeService;
import com.inventory.service.InventoryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/barcode")
public class BarcodeController {
    private final BarcodeService barcodeService;
    private final BarcodeGenerator barcodeGenerator;
    private final InventoryService inventoryService;
    private final AllocationService allocationService;

    public BarcodeController(BarcodeService barcodeService, BarcodeGenerator barcodeGenerator,
                              InventoryService inventoryService, AllocationService allocationService) {
        this.barcodeService = barcodeService;
        this.barcodeGenerator = barcodeGenerator;
        this.inventoryService = inventoryService;
        this.allocationService = allocationService;
    }

    // Preview/utility generator - not tied to a database record. Real barcodes are
    // assigned server-side: inventory items get one automatically from their category's
    // sequence counter (InventoryService.create), and allocations get a composite one
    // (AllocationService.allocate).
    @GetMapping("/new")
    public Map<String, String> generate(@RequestParam(defaultValue = "INV") String prefix) {
        String value = barcodeGenerator.generateInventoryBarcode(prefix);
        return Collections.singletonMap("barcode", value);
    }

    // Unified "scan" endpoint: recognizes both an inventory item's own barcode
    // (e.g. "ELEC-0007") and a composite allocation barcode
    // (e.g. "2026-ELEC-ELEC-0007-IT-9F3D2A"), and returns whichever matched.
    @GetMapping("/scan/{barcode}")
    public ResponseEntity<ScanResultDTO> scan(@PathVariable String barcode) {
        String normalized = barcode.trim();

        Optional<InventoryDTO> inventory = inventoryService.tryFindByBarcode(normalized);
        if (inventory.isPresent()) {
            return ResponseEntity.ok(new ScanResultDTO("INVENTORY", inventory.get(), null));
        }

        Optional<AllocationDTO> allocation = allocationService.tryFindByAllocationBarcode(normalized.toUpperCase());
        if (allocation.isPresent()) {
            return ResponseEntity.ok(new ScanResultDTO("ALLOCATION", null, allocation.get()));
        }

        throw new ResourceNotFoundException("No inventory item or allocation found for this barcode");
    }

    // Public: rendered directly in <img> tags, which don't send an Authorization header.
    // Works for any string - inventory barcodes, category codes, department codes, and
    // composite allocation barcodes all render through this same endpoint.
    @GetMapping("/image/{text}")
    public ResponseEntity<byte[]> image(@PathVariable String text) throws IOException, WriterException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(MediaType.IMAGE_PNG)
                .body(barcodeService.generateBarcode(text));
    }
}
