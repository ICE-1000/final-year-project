package com.inventory.service;

import com.inventory.reports.ExcelService;
import com.inventory.reports.PdfService;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ReportService {
    private final InventoryService inventoryService;
    private final PdfService pdfService;
    private final ExcelService excelService;

    public ReportService(InventoryService inventoryService, PdfService pdfService, ExcelService excelService) {
        this.inventoryService = inventoryService;
        this.pdfService = pdfService;
        this.excelService = excelService;
    }

    public byte[] inventoryPdf() {
        return pdfService.generateInventoryReport(inventoryService.findAll());
    }

    public byte[] inventoryExcel() throws IOException {
        return excelService.generateInventoryReport(inventoryService.findAll());
    }
}
