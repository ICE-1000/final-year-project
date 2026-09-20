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

    // CHANGED: added Brand/Specification columns.
    public byte[] generateInventoryReport(List<InventoryDTO> list) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Barcode");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Category");
            header.createCell(3).setCellValue("Brand");
            header.createCell(4).setCellValue("Specification");
            header.createCell(5).setCellValue("Quantity");
            header.createCell(6).setCellValue("Available");
            header.createCell(7).setCellValue("Allocated");
            header.createCell(8).setCellValue("Status");
            for (int i = 0; i < list.size(); i++) {
                InventoryDTO item = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(safe(item.getBarcode()));
                row.createCell(1).setCellValue(safe(item.getInventoryName()));
                row.createCell(2).setCellValue(safe(item.getCategoryName()));
                row.createCell(3).setCellValue(safe(item.getBrand()));
                row.createCell(4).setCellValue(safe(item.getSpecification()));
                row.createCell(5).setCellValue(item.getQuantity());
                row.createCell(6).setCellValue(item.getAvailableQuantity() == null ? 0 : item.getAvailableQuantity());
                row.createCell(7).setCellValue(item.getAllocatedQuantity() == null ? 0 : item.getAllocatedQuantity());
                row.createCell(8).setCellValue(safe(item.getStatus()));
            }
            for (int col = 0; col <= 8; col++) {
                sheet.autoSizeColumn(col);
            }
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

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

    private String safeSheetName(String code) {
        String base = (code == null || code.isBlank()) ? "Department" : code;
        String cleaned = base.replaceAll("[:\\\\/?*\\[\\]]", "");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
