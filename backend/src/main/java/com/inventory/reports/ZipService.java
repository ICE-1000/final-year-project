package com.inventory.reports;

import com.google.zxing.WriterException;
import com.inventory.dto.InventoryUnitDTO;
import com.inventory.service.BarcodeService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {
    private final BarcodeService barcodeService;

    public ZipService(BarcodeService barcodeService) {
        this.barcodeService = barcodeService;
    }

    // One PNG per unit, named "{n}-{unitBarcode}.png", nested inside a folder per
    // section (allocation or inventory group) using each section's short label - so
    // extracting the zip keeps different allocations/groups in clearly separate
    // folders rather than one big mixed pile of files.
    public byte[] generateBarcodeZip(List<PdfService.BarcodeSection> sections) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (PdfService.BarcodeSection section : sections) {
                String folder = sanitizeFolderName(section.getLabel());
                int i = 1;
                for (InventoryUnitDTO unit : section.getUnits()) {
                    try {
                        byte[] png = barcodeService.generateBarcode(unit.getUnitBarcode());
                        ZipEntry entry = new ZipEntry(folder + "/" + String.format("%03d", i) + "-" + unit.getUnitBarcode() + ".png");
                        zos.putNextEntry(entry);
                        zos.write(png);
                        zos.closeEntry();
                        i++;
                    } catch (WriterException wex) {
                        throw new IOException("Failed to render barcode image for " + unit.getUnitBarcode(), wex);
                    }
                }
            }
        }
        return baos.toByteArray();
    }

    private String sanitizeFolderName(String label) {
        String base = (label == null || label.isBlank()) ? "barcodes" : label;
        String cleaned = base.replaceAll("[^A-Za-z0-9 \\-]", "").replaceAll("\\s+", "-").trim();
        if (cleaned.length() > 60) {
            cleaned = cleaned.substring(0, 60);
        }
        return cleaned.isEmpty() ? "barcodes" : cleaned;
    }
}
