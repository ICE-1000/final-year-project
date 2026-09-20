package com.inventory.controller;

import com.inventory.model.User;
import com.inventory.service.ReportService;
import com.inventory.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    private final UserService userService;

    public ReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping("/inventory.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> inventoryPdf() throws IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportService.inventoryPdf());
    }

    @GetMapping("/inventory.xlsx")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> inventoryExcel() throws IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reportService.inventoryExcel());
    }

    @GetMapping("/department.pdf")
    @PreAuthorize("hasRole('DEPARTMENT') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> departmentPdf(@RequestParam(required = false) UUID departmentId) throws IOException {
        User currentUser = userService.getCurrentUser();
        byte[] report = reportService.departmentReportPdf(currentUser, departmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=department-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report);
    }

    @GetMapping("/department.xlsx")
    @PreAuthorize("hasRole('DEPARTMENT') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> departmentExcel(@RequestParam(required = false) UUID departmentId) throws IOException {
        User currentUser = userService.getCurrentUser();
        byte[] report = reportService.departmentReportExcel(currentUser, departmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=department-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(report);
    }

    // NEW: printable barcode label sheet, one section per allocation (or a single
    // section if allocationId is given), each section its own page(s) so different
    // allocations' labels are never mixed together once printed.
    @GetMapping("/allocation-barcodes.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> allocationBarcodesPdf(@RequestParam(required = false) UUID allocationId) throws IOException {
        byte[] report = reportService.allocationBarcodesPdf(allocationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=allocation-barcodes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report);
    }

    // NEW: same barcode set as above, as a ZIP of individual PNGs organized into one
    // folder per allocation.
    @GetMapping("/allocation-barcodes.zip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> allocationBarcodesZip(@RequestParam(required = false) UUID allocationId) throws IOException {
        byte[] report = reportService.allocationBarcodesZip(allocationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=allocation-barcodes.zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(report);
    }

    // NEW: printable barcode label sheet for every unit in one inventory group -
    // typically used right after registering a batch of stock.
    @GetMapping("/inventory-barcodes.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> inventoryBarcodesPdf(@RequestParam UUID inventoryId) throws IOException {
        byte[] report = reportService.inventoryBarcodesPdf(inventoryId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-barcodes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report);
    }

    // NEW: same barcode set as above, as a ZIP of individual PNGs.
    @GetMapping("/inventory-barcodes.zip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> inventoryBarcodesZip(@RequestParam UUID inventoryId) throws IOException {
        byte[] report = reportService.inventoryBarcodesZip(inventoryId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-barcodes.zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(report);
    }
}
