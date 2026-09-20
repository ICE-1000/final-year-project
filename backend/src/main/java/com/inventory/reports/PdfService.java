package com.inventory.reports;

import com.google.zxing.WriterException;
import com.inventory.dto.AllocationDTO;
import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryUnitDTO;
import com.inventory.model.Department;
import com.inventory.service.BarcodeService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Real-world label proportions: a barcode rendered to fit roughly 65mm wide by
    // 22mm tall (185pt x 62pt at 72pt/inch) - a common thermal/adhesive label size -
    // with the human-readable unit code and, if known, its batch reference printed
    // beneath it inside the same bordered cell, so cutting along the grid lines
    // produces a usable physical label rather than an arbitrarily-sized screenshot of
    // a barcode.
    private static final float LABEL_IMAGE_WIDTH_PT = 185f;
    private static final float LABEL_IMAGE_HEIGHT_PT = 62f;
    private static final Font CODE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font BATCH_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, new Color(100, 100, 100));

    private final BarcodeService barcodeService;

    public PdfService(BarcodeService barcodeService) {
        this.barcodeService = barcodeService;
    }

    public byte[] generateInventoryReport(List<InventoryDTO> data) throws IOException {
        Document document = new Document();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try {
                PdfWriter.getInstance(document, baos);
                document.open();
                document.add(new Paragraph("THE UNIVERSITY OF ZAMBIA (UNZA) - INVENTORY REPORT"));
                document.add(new Paragraph(" "));
                PdfPTable table = new PdfPTable(6);
                table.addCell("Barcode");
                table.addCell("Name");
                table.addCell("Brand");
                table.addCell("Specification");
                table.addCell("Quantity");
                table.addCell("Status");
                for (InventoryDTO item : data) {
                    table.addCell(safe(item.getBarcode()));
                    table.addCell(safe(item.getInventoryName()));
                    table.addCell(safe(item.getBrand()));
                    table.addCell(safe(item.getSpecification()));
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

    // A printable sheet of individual unit barcodes, one section per group of units
    // (an allocation, or a single inventory group), each section starting with a clear
    // text header and its own page break before the next section - so if you print this
    // and cut the labels apart, units from different allocations/groups are never mixed.
    // Each label is a bordered cell sized to real-world label proportions, showing the
    // barcode image, the unit's own code, and (if known) which registration batch it
    // came from.
    public byte[] generateBarcodeLabelSheet(String reportTitle, List<BarcodeSection> sections) throws IOException {
        Document document = new Document();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try {
                PdfWriter.getInstance(document, baos);
                document.open();
                document.add(new Paragraph("THE UNIVERSITY OF ZAMBIA (UNZA) - " + reportTitle));
                document.add(new Paragraph(" "));

                if (sections.isEmpty()) {
                    document.add(new Paragraph("Nothing to print."));
                }

                for (int i = 0; i < sections.size(); i++) {
                    BarcodeSection section = sections.get(i);
                    document.add(new Paragraph(section.getHeader()));
                    document.add(new Paragraph(" "));

                    if (section.getUnits().isEmpty()) {
                        document.add(new Paragraph("No individually-tracked units found."));
                    } else {
                        PdfPTable grid = new PdfPTable(2);
                        grid.setWidthPercentage(100);
                        for (InventoryUnitDTO unit : section.getUnits()) {
                            grid.addCell(buildLabelCell(unit));
                        }
                        document.add(grid);
                    }

                    if (i < sections.size() - 1) {
                        document.newPage();
                    }
                }
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

    private PdfPCell buildLabelCell(InventoryUnitDTO unit) throws IOException {
        try {
            byte[] png = barcodeService.generateBarcode(unit.getUnitBarcode());
            Image img = Image.getInstance(png);
            img.scaleToFit(LABEL_IMAGE_WIDTH_PT, LABEL_IMAGE_HEIGHT_PT);

            PdfPCell cell = new PdfPCell();
            cell.setPadding(10f);
            cell.setBorder(Rectangle.BOX);
            cell.setBorderColor(new Color(180, 180, 180));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph imgPara = new Paragraph();
            imgPara.add(img);
            imgPara.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(imgPara);

            Paragraph codeLine = new Paragraph(unit.getUnitBarcode(), CODE_FONT);
            codeLine.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(codeLine);

            if (unit.getBatchCode() != null && !unit.getBatchCode().isBlank()) {
                Paragraph batchLine = new Paragraph("Batch: " + unit.getBatchCode(), BATCH_FONT);
                batchLine.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(batchLine);
            }
            return cell;
        } catch (WriterException wex) {
            throw new IOException("Failed to render barcode image for " + unit.getUnitBarcode(), wex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class BarcodeSection {
        private final String header;
        private final String label;
        private final List<InventoryUnitDTO> units;

        public BarcodeSection(String header, String label, List<InventoryUnitDTO> units) {
            this.header = header;
            this.label = label;
            this.units = units;
        }

        public String getHeader() { return header; }
        public String getLabel() { return label; }
        public List<InventoryUnitDTO> getUnits() { return units; }
    }
}
