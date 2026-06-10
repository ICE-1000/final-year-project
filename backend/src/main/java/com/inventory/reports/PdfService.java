package com.inventory.reports;

import com.inventory.dto.InventoryDTO;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfService {
    public byte[] generateInventoryReport(List<InventoryDTO> data) {
        Document document = new Document();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        document.add(new Paragraph(" THE UNIVERSITY OF ZAMBIA(UNZA) INVENTORY REPORT"));
        PdfPTable table = new PdfPTable(4);
        table.addCell("Barcode");
        table.addCell("Name");
        table.addCell("Quantity");
        table.addCell("Status");
        for (InventoryDTO item : data) {
            table.addCell(item.getBarcode());
            table.addCell(item.getInventoryName());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(item.getStatus());
        }
        document.add(table);
        document.close();
        return baos.toByteArray();
    }
}
