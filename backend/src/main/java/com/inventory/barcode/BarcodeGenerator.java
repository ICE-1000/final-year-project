package com.inventory.barcode;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class BarcodeGenerator {
    public String generateInventoryBarcode(String prefix) {
        String safePrefix = prefix == null || prefix.trim().isEmpty() ? "INV" : prefix.trim().toUpperCase();
        return safePrefix + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
