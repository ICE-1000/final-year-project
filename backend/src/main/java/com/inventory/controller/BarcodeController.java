package com.inventory.controller;

import com.google.zxing.WriterException;
import com.inventory.barcode.BarcodeGenerator;
import com.inventory.service.BarcodeService;
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

@RestController
@RequestMapping("/api/barcode")
public class BarcodeController {
    private final BarcodeService barcodeService;
    private final BarcodeGenerator barcodeGenerator;

    public BarcodeController(BarcodeService barcodeService, BarcodeGenerator barcodeGenerator) {
        this.barcodeService = barcodeService;
        this.barcodeGenerator = barcodeGenerator;
    }

    @GetMapping("/new")
    public Map<String, String> generate(@RequestParam(defaultValue = "INV") String prefix) {
        String value = barcodeGenerator.generateInventoryBarcode(prefix);
        return Collections.singletonMap("barcode", value);
    }

    @GetMapping("/image/{text}")
    public ResponseEntity<byte[]> image(@PathVariable String text) throws IOException, WriterException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(MediaType.IMAGE_PNG)
                .body(barcodeService.generateBarcode(text));
    }
}
