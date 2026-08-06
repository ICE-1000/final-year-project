package com.inventory.reports;

import com.inventory.dto.AllocationDTO;
import com.inventory.dto.InventoryDTO;
import com.inventory.model.Department;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Admin: full system inventory report.
    public byte[] generateInventoryReport(List<InventoryDTO> list) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Barcode");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Category");
            header.createCell(3).setCellValue("Quantity");
            header.createCell(4).setCellValue("Available");
            header.createCell(5).setCellValue("Allocated");
            header.createCell(6).setCellValue("Status");
            for (int i = 0; i < list.size(); i++) {
                InventoryDTO item = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(safe(item.getBarcode()));
                row.createCell(1).setCellValue(safe(item.getInventoryName()));
                row.createCell(2).setCellValue(safe(item.getCategoryName()));
                row.createCell(3).setCellValue(item.getQuantity());
                row.createCell(4).setCellValue(item.getAvailableQuantity());
                row.createCell(5).setCellValue(item.getAllocatedQuantity());
                row.createCell(6).setCellValue(safe(item.getStatus()));
            }
            for (int col = 0; col <= 6; col++) {
                sheet.autoSizeColumn(col);
            }
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // Department: allocation history scoped to a single department.
    // Layout/branding to be refined later - this establishes a working, null-safe baseline.
    public byte[] generateDepartmentReport(Department department, List<AllocationDTO> allocations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(department.getDepartmentCode()));

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Department: " + safe(department.getDepartmentName())
                    + " (" + safe(department.getDepartmentCode()) + ")");

            Row header = sheet.createRow(2);
            header.createCell(0).setCellValue("Item");
            header.createCell(1).setCellValue("Quantity");
            header.createCell(2).setCellValue("Status");
            header.createCell(3).setCellValue("Allocated At");
            header.createCell(4).setCellValue("Allocation ID");

            for (int i = 0; i < allocations.size(); i++) {
                AllocationDTO allocation = allocations.get(i);
                Row row = sheet.createRow(i + 3);
                row.createCell(0).setCellValue(safe(allocation.getInventoryName()));
                row.createCell(1).setCellValue(allocation.getQuantity());
                row.createCell(2).setCellValue(safe(allocation.getStatus()));
                row.createCell(3).setCellValue(allocation.getAllocatedAt() == null ? "" : allocation.getAllocatedAt().format(DATE_FORMAT));
                row.createCell(4).setCellValue(allocation.getId() == null ? "" : allocation.getId().toString());
            }
            for (int col = 0; col <= 4; col++) {
                sheet.autoSizeColumn(col);
            }
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    // Excel sheet names can't be blank, exceed 31 chars, or contain : \ / ? * [ ]
    private String safeSheetName(String code) {
        String base = (code == null || code.isBlank()) ? "Department" : code;
        String cleaned = base.replaceAll("[:\\\\/?*\\[\\]]", "");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
