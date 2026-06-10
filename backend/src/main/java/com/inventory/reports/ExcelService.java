package com.inventory.reports;

import com.inventory.dto.InventoryDTO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelService {
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
                row.createCell(0).setCellValue(item.getBarcode());
                row.createCell(1).setCellValue(item.getInventoryName());
                row.createCell(2).setCellValue(item.getCategory());
                row.createCell(3).setCellValue(item.getQuantity());
                row.createCell(4).setCellValue(item.getAvailableQuantity());
                row.createCell(5).setCellValue(item.getAllocatedQuantity());
                row.createCell(6).setCellValue(item.getStatus());
            }
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}
