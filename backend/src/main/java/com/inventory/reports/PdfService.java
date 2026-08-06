package com.inventory.reports;

import com.inventory.dto.AllocationDTO;
import com.inventory.dto.InventoryDTO;
import com.inventory.model.Department;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Admin: full system inventory report.
    public byte[] generateInventoryReport(List<InventoryDTO> data) throws IOException {
        Document document = new Document();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try {
                PdfWriter.getInstance(document, baos);
                document.open();
                document.add(new Paragraph("THE UNIVERSITY OF ZAMBIA (UNZA) - INVENTORY REPORT"));
                document.add(new Paragraph(" "));
                PdfPTable table = new PdfPTable(4);
                table.addCell("Barcode");
                table.addCell("Name");
                table.addCell("Quantity");
                table.addCell("Status");
                for (InventoryDTO item : data) {
                    table.addCell(safe(item.getBarcode()));
                    table.addCell(safe(item.getInventoryName()));
                    table.addCell(String.valueOf(item.getQuantity()));
                    table.addCell(safe(item.getStatus()));
                }
                document.add(table);
            } catch (DocumentException ex) {
                throw new IOException("Failed to generate PDF report", ex);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
            return baos.toByteArray();
        }
    }

    // Department: allocation history scoped to a single department.
    // Layout/branding to be refined later - this establishes a working, null-safe baseline.
    public byte[] generateDepartmentReport(Department department, List<AllocationDTO> allocations) throws IOException {
        Document document = new Document();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try {
                PdfWriter.getInstance(document, baos);
                document.open();
                document.add(new Paragraph("THE UNIVERSITY OF ZAMBIA (UNZA) - DEPARTMENT ALLOCATION REPORT"));
                document.add(new Paragraph("Department: " + safe(department.getDepartmentName())
                        + " (" + safe(department.getDepartmentCode()) + ")"));
                document.add(new Paragraph(" "));
                PdfPTable table = new PdfPTable(5);
                table.addCell("Item");
                table.addCell("Quantity");
                table.addCell("Status");
                table.addCell("Allocated At");
                table.addCell("Allocation ID");
                for (AllocationDTO allocation : allocations) {
                    table.addCell(safe(allocation.getInventoryName()));
                    table.addCell(String.valueOf(allocation.getQuantity()));
                    table.addCell(safe(allocation.getStatus()));
                    table.addCell(allocation.getAllocatedAt() == null ? "" : allocation.getAllocatedAt().format(DATE_FORMAT));
                    table.addCell(allocation.getId() == null ? "" : allocation.getId().toString());
                }
                document.add(table);
            } catch (DocumentException ex) {
                throw new IOException("Failed to generate PDF report", ex);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
            return baos.toByteArray();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
