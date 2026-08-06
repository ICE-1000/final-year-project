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

    // Admin: full system inventory report across all departments.
    @GetMapping("/inventory.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> inventoryPdf() {
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

    // Department: allocation report scoped to exactly one department.
    // A DEPARTMENT caller always gets their own department - departmentId is ignored/rejected
    // if it doesn't match their own. An ADMIN may pass departmentId to pull any department's report.
    @GetMapping("/department.pdf")
    @PreAuthorize("hasRole('DEPARTMENT') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> departmentPdf(@RequestParam(required = false) UUID departmentId) {
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
}
