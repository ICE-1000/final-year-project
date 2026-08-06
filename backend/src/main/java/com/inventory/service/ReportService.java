package com.inventory.service;

import com.inventory.dto.AllocationDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.model.Department;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.reports.ExcelService;
import com.inventory.reports.PdfService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {
    private final InventoryService inventoryService;
    private final AllocationService allocationService;
    private final DepartmentService departmentService;
    private final PdfService pdfService;
    private final ExcelService excelService;

    public ReportService(InventoryService inventoryService, AllocationService allocationService,
                          DepartmentService departmentService, PdfService pdfService, ExcelService excelService) {
        this.inventoryService = inventoryService;
        this.allocationService = allocationService;
        this.departmentService = departmentService;
        this.pdfService = pdfService;
        this.excelService = excelService;
    }

    // Admin: full system inventory report (all items across all departments).
    public byte[] inventoryPdf() throws IOException {
        return pdfService.generateInventoryReport(inventoryService.findAll());
    }

    public byte[] inventoryExcel() throws IOException {
        return excelService.generateInventoryReport(inventoryService.findAll());
    }

    // Department: allocation report scoped to exactly one department.
    // A DEPARTMENT user always gets their own department, regardless of what (if anything)
    // they pass in departmentId - there is no way to request another department's data.
    // An ADMIN may pull any department's report by passing its id explicitly.
    public byte[] departmentReportPdf(User currentUser, UUID requestedDepartmentId) throws IOException {
        UUID departmentId = resolveDepartmentId(currentUser, requestedDepartmentId);
        Department department = departmentService.get(departmentId);
        List<AllocationDTO> allocations = allocationService.findByDepartment(departmentId);
        return pdfService.generateDepartmentReport(department, allocations);
    }

    public byte[] departmentReportExcel(User currentUser, UUID requestedDepartmentId) throws IOException {
        UUID departmentId = resolveDepartmentId(currentUser, requestedDepartmentId);
        Department department = departmentService.get(departmentId);
        List<AllocationDTO> allocations = allocationService.findByDepartment(departmentId);
        return excelService.generateDepartmentReport(department, allocations);
    }

    private UUID resolveDepartmentId(User currentUser, UUID requestedDepartmentId) {
        if (currentUser.getRole() == Role.ADMIN) {
            if (requestedDepartmentId == null) {
                throw new BadRequestException("departmentId is required for admin department reports");
            }
            return requestedDepartmentId;
        }
        if (currentUser.getDepartment() == null) {
            throw new BadRequestException("User is not assigned to a department");
        }
        UUID ownDepartmentId = currentUser.getDepartment().getId();
        if (requestedDepartmentId != null && !requestedDepartmentId.equals(ownDepartmentId)) {
            throw new AccessDeniedException("You can only access your own department's report");
        }
        return ownDepartmentId;
    }
}
