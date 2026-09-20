package com.inventory.controller;

import com.google.zxing.WriterException;
import com.inventory.barcode.BarcodeGenerator;
import com.inventory.dto.InventoryUnitDTO;
import com.inventory.dto.ScanResultDTO;
import com.inventory.exception.ResourceNotFoundException;
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

    public BarcodeController(BarcodeService barcodeService, BarcodeGenerator barcodeGenerator,
                              InventoryService inventoryService) {
        this.barcodeService = barcodeService;
        this.barcodeGenerator = barcodeGenerator;
        this.inventoryService = inventoryService;
    }

    // Preview/utility generator - not tied to a database record.
    @GetMapping("/new")
    public Map<String, String> generate(@RequestParam(defaultValue = "INV") String prefix) {
        String value = barcodeGenerator.generateInventoryBarcode(prefix);
        return Collections.singletonMap("barcode", value);
    }

    // Every physical item has exactly one permanent barcode, assigned once at
    // registration and reused for its whole lifecycle - scanning it always returns the
    // full picture (group details, brand/specification, condition, current status, and
    // - if currently allocated - which department and when).
    @GetMapping("/scan/{barcode}")
    public ResponseEntity<ScanResultDTO> scan(@PathVariable String barcode) {
        Optional<InventoryUnitDTO> unit = inventoryService.tryFindUnitByBarcode(barcode);
        if (unit.isPresent()) {
            return ResponseEntity.ok(new ScanResultDTO("UNIT", unit.get()));
        }
        throw new ResourceNotFoundException("No inventory unit found for this barcode");
    }

    // Public: rendered directly in <img> tags, which don't send an Authorization header.
    // Works for any string - unit barcodes, category codes, department codes.
    @GetMapping("/image/{text}")
    public ResponseEntity<byte[]> image(@PathVariable String text) throws IOException, WriterException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(MediaType.IMAGE_PNG)
                .body(barcodeService.generateBarcode(text));
    }
}
