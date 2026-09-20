package com.inventory.service;

import com.inventory.dto.AllocationDTO;
import com.inventory.dto.InventoryDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Department;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.reports.ExcelService;
import com.inventory.reports.PdfService;
import com.inventory.reports.ZipService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final InventoryService inventoryService;
    private final AllocationService allocationService;
    private final DepartmentService departmentService;
    private final PdfService pdfService;
    private final ExcelService excelService;
    private final ZipService zipService;

    public ReportService(InventoryService inventoryService, AllocationService allocationService,
                          DepartmentService departmentService, PdfService pdfService, ExcelService excelService,
                          ZipService zipService) {
        this.inventoryService = inventoryService;
        this.allocationService = allocationService;
        this.departmentService = departmentService;
        this.pdfService = pdfService;
        this.excelService = excelService;
        this.zipService = zipService;
    }

    public byte[] inventoryPdf() throws IOException {
        return pdfService.generateInventoryReport(inventoryService.findAll());
    }

    public byte[] inventoryExcel() throws IOException {
        return excelService.generateInventoryReport(inventoryService.findAll());
    }

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

    public byte[] allocationBarcodesPdf(UUID allocationId) throws IOException {
        return pdfService.generateBarcodeLabelSheet("ALLOCATION BARCODE LABELS", buildAllocationSections(allocationId));
    }

    public byte[] allocationBarcodesZip(UUID allocationId) throws IOException {
        return zipService.generateBarcodeZip(buildAllocationSections(allocationId));
    }

    public byte[] inventoryBarcodesPdf(UUID inventoryId) throws IOException {
        return pdfService.generateBarcodeLabelSheet("INVENTORY BARCODE LABELS", buildInventorySections(inventoryId));
    }

    public byte[] inventoryBarcodesZip(UUID inventoryId) throws IOException {
        return zipService.generateBarcodeZip(buildInventorySections(inventoryId));
    }

    // Shared by both the PDF and ZIP endpoints, so the two formats always show exactly
    // the same set of allocations/units and section grouping.
    private List<PdfService.BarcodeSection> buildAllocationSections(UUID allocationId) {
        List<AllocationDTO> allocations = allocationService.findAll();
        if (allocationId != null) {
            allocations = allocations.stream()
                    .filter(a -> a.getId().equals(allocationId))
                    .collect(Collectors.toList());
            if (allocations.isEmpty()) {
                throw new ResourceNotFoundException("Allocation not found");
            }
        }
        return allocations.stream()
                .map(a -> new PdfService.BarcodeSection(
                        "Allocation: " + safe(a.getInventoryName())
                                + "  |  Department: " + safe(a.getDepartmentName())
                                + "  |  Quantity: " + a.getQuantity()
                                + "  |  Date: " + (a.getAllocatedAt() == null ? "" : a.getAllocatedAt().toString()),
                        safe(a.getDepartmentName()) + "-" + safe(a.getInventoryName()),
                        inventoryService.findUnitsByAllocation(a.getId())))
                .collect(Collectors.toList());
    }

    private List<PdfService.BarcodeSection> buildInventorySections(UUID inventoryId) {
        InventoryDTO group = inventoryService.getById(inventoryId);
        String header = "Item: " + safe(group.getInventoryName())
                + (group.getBrand() != null && !group.getBrand().isBlank() ? " (" + group.getBrand() + ")" : "")
                + "  |  Category: " + safe(group.getCategoryName())
                + "  |  Base Code: " + safe(group.getBarcode());
        String label = safe(group.getInventoryName()) + (group.getBrand() != null ? "-" + group.getBrand() : "");
        return List.of(new PdfService.BarcodeSection(header, label, inventoryService.findUnitsByInventory(inventoryId)));
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
