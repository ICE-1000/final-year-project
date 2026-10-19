package com.inventory.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.inventory.dto.AllocationDTO;
import com.inventory.dto.AllocationRequest;
import com.inventory.dto.CategoryDTO;
import com.inventory.dto.CreateRequestDTO;
import com.inventory.dto.DepartmentDTO;
import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryRequestDTO;
import com.inventory.dto.InventoryUnitDTO;
import com.inventory.dto.UpdateRequestStatusDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.model.Role;
import com.inventory.model.RequestStatus;
import com.inventory.model.User;
import com.inventory.service.AllocationService;
import com.inventory.service.CategoryService;
import com.inventory.service.DepartmentService;
import com.inventory.service.InventoryRequestService;
import com.inventory.service.InventoryService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Assistant tools and role permissions.
@Component
public class AgentToolRegistry {
    private static final int MAX_RESULTS = 25;
    private static final Set<Role> ADMIN_ONLY = Set.of(Role.ADMIN);
    private static final Set<Role> DEPARTMENT_ONLY = Set.of(Role.DEPARTMENT);
    private static final Set<Role> ANY_ROLE = Set.of(Role.ADMIN, Role.DEPARTMENT);

    private final InventoryService inventoryService;
    private final CategoryService categoryService;
    private final DepartmentService departmentService;
    private final AllocationService allocationService;
    private final InventoryRequestService inventoryRequestService;

    private final List<AgentTool> tools = new ArrayList<>();

    public AgentToolRegistry(InventoryService inventoryService, CategoryService categoryService,
                              DepartmentService departmentService, AllocationService allocationService,
                              InventoryRequestService inventoryRequestService) {
        this.inventoryService = inventoryService;
        this.categoryService = categoryService;
        this.departmentService = departmentService;
        this.allocationService = allocationService;
        this.inventoryRequestService = inventoryRequestService;
        registerTools();
    }

    public List<AgentTool> toolsForRole(Role role) {
        return tools.stream().filter(t -> t.getAllowedRoles().contains(role)).collect(Collectors.toList());
    }

    public Optional<AgentTool> find(String name) {
        return tools.stream().filter(t -> t.getName().equals(name)).findFirst();
    }

    // ------------------------------------------------------------------
    // Tool registration
    // ------------------------------------------------------------------

    private void registerTools() {
        tools.add(new AgentTool(
                "list_inventory",
                "List registered inventory items (groups), optionally filtered by a text search across "
                        + "name/brand/specification and/or a category name. Returns each matching item's name, "
                        + "brand, specification, category, base barcode, total/available/allocated quantity, and status. "
                        + "Use this before add_inventory or allocate_inventory to check what already exists.",
                schema(Map.of(
                        "query", prop("string", "Optional free-text search across item name, brand, and specification."),
                        "categoryName", prop("string", "Optional category name to filter by."),
                        "onlyAvailable", prop("boolean", "If true, only return items with available stock (availableQuantity > 0).")
                ), List.of()),
                ANY_ROLE,
                this::listInventory
        ));

        tools.add(new AgentTool(
                "get_inventory_stats",
                "Get system-wide inventory totals: total items, available, allocated, and how many are low on stock.",
                schema(Map.of(), List.of()),
                ANY_ROLE,
                (args, user) -> inventoryService.stats()
        ));

        tools.add(new AgentTool(
                "get_categories",
                "List all inventory categories with their name, short code, and description.",
                schema(Map.of(), List.of()),
                ANY_ROLE,
                (args, user) -> categoryService.findAll().stream().map(this::categorySummary).collect(Collectors.toList())
        ));

        tools.add(new AgentTool(
                "scan_barcode",
                "Look up a single physical inventory unit by its exact barcode (e.g. from a scan or a printed "
                        + "label) and return its full details: item name, category, brand, specification, condition, "
                        + "status, batch, and current allocation if any.",
                schema(Map.of(
                        "barcode", prop("string", "The exact barcode text to look up.")
                ), List.of("barcode")),
                ANY_ROLE,
                this::scanBarcode
        ));

        // --- ADMIN-only: write actions and system-wide reads ---

        tools.add(new AgentTool(
                "add_inventory",
                "Register new inventory stock, or add more stock to an existing item (same category + name + "
                        + "brand + specification merges into the existing item and mints new individually-barcoded "
                        + "units continuing its numbering, rather than creating a duplicate). Call get_categories "
                        + "first if you don't already know the exact category name.",
                schema(Map.of(
                        "categoryName", prop("string", "Exact or close match of an existing category's name."),
                        "itemName", prop("string", "The item's name, e.g. 'Electric Cable'."),
                        "brand", prop("string", "Optional brand, e.g. 'ZESCO'."),
                        "specification", prop("string", "Optional specification, e.g. '2.5mm Copper'."),
                        "quantity", prop("integer", "How many units to register now."),
                        "description", prop("string", "Optional description."),
                        "condition", prop("string", "Optional condition note, e.g. 'New'.")
                ), List.of("categoryName", "itemName", "quantity")),
                ADMIN_ONLY,
                this::addInventory
        ));

        tools.add(new AgentTool(
                "add_category",
                "Create a new inventory category with a short unique code (2-10 letters/numbers, e.g. 'ELEC').",
                schema(Map.of(
                        "name", prop("string", "Category name, e.g. 'Electrical Equipment'."),
                        "code", prop("string", "Short unique code, 2-10 letters/numbers, e.g. 'ELEC'."),
                        "description", prop("string", "Optional description.")
                ), List.of("name", "code")),
                ADMIN_ONLY,
                this::addCategory
        ));

        tools.add(new AgentTool(
                "get_departments",
                "List all departments with their name and short code.",
                schema(Map.of(), List.of()),
                ADMIN_ONLY,
                (args, user) -> departmentService.findAll().stream().map(this::departmentSummary).collect(Collectors.toList())
        ));

        tools.add(new AgentTool(
                "allocate_inventory",
                "Allocate a quantity of an inventory item to a department - pulls that many specific available "
                        + "units and marks them allocated. Call list_inventory and get_departments first if you "
                        + "don't already know the exact item/department names.",
                schema(Map.of(
                        "itemName", prop("string", "Name of the inventory item to allocate from."),
                        "brand", prop("string", "Optional brand, to disambiguate if the same item name has multiple brands."),
                        "departmentName", prop("string", "Name of the department to allocate to."),
                        "quantity", prop("integer", "How many units to allocate.")
                ), List.of("itemName", "departmentName", "quantity")),
                ADMIN_ONLY,
                this::allocateInventory
        ));

        tools.add(new AgentTool(
                "get_all_allocations",
                "List recent allocations across all departments - item, department, quantity, status, date.",
                schema(Map.of(), List.of()),
                ADMIN_ONLY,
                (args, user) -> capList(allocationService.findAll().stream()
                        .sorted(Comparator.comparing(AllocationDTO::getAllocatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(this::allocationSummary).collect(Collectors.toList()))
        ));

        tools.add(new AgentTool(
                "get_requests",
                "List inventory requests submitted by departments, optionally filtered by status "
                        + "(PENDING, APPROVED, or REJECTED). Call this before approve_request/reject_request to "
                        + "find the requestId you need.",
                schema(Map.of(
                        "status", prop("string", "Optional filter: PENDING, APPROVED, or REJECTED.")
                ), List.of()),
                ADMIN_ONLY,
                this::getRequests
        ));

        tools.add(new AgentTool(
                "approve_request",
                "Approve a pending inventory request. Requires the exact requestId (from get_requests).",
                schema(Map.of(
                        "requestId", prop("string", "The request's id, as returned by get_requests.")
                ), List.of("requestId")),
                ADMIN_ONLY,
                this::approveRequest
        ));

        tools.add(new AgentTool(
                "reject_request",
                "Reject a pending inventory request with a reason. Requires the exact requestId (from get_requests).",
                schema(Map.of(
                        "requestId", prop("string", "The request's id, as returned by get_requests."),
                        "reason", prop("string", "Why the request is being rejected - required.")
                ), List.of("requestId", "reason")),
                ADMIN_ONLY,
                this::rejectRequest
        ));

        // --- DEPARTMENT-only: always scoped to the caller's own department ---

        tools.add(new AgentTool(
                "get_my_allocations",
                "List inventory currently allocated to MY department - item, quantity, status, date.",
                schema(Map.of(), List.of()),
                DEPARTMENT_ONLY,
                (args, user) -> capList(allocationService.findByDepartment(requireOwnDepartmentId(user)).stream()
                        .map(this::allocationSummary).collect(Collectors.toList()))
        ));

        tools.add(new AgentTool(
                "get_my_requests",
                "List inventory requests MY department has submitted, with their current status.",
                schema(Map.of(), List.of()),
                DEPARTMENT_ONLY,
                (args, user) -> capList(inventoryRequestService.getRequestsForDepartment(requireOwnDepartmentId(user)).stream()
                        .map(this::requestSummary).collect(Collectors.toList()))
        ));

        tools.add(new AgentTool(
                "request_inventory",
                "Submit a new inventory request on behalf of MY department. Call get_categories first if you "
                        + "don't already know the exact category name, and list_inventory to check what's already "
                        + "available before requesting more of it.",
                schema(Map.of(
                        "categoryName", prop("string", "Exact or close match of an existing category's name."),
                        "itemName", prop("string", "What's being requested."),
                        "quantity", prop("integer", "How many are needed."),
                        "neededBy", prop("string", "Date needed by, in YYYY-MM-DD format - must be today or later."),
                        "description", prop("string", "Optional extra detail.")
                ), List.of("categoryName", "itemName", "quantity", "neededBy")),
                DEPARTMENT_ONLY,
                this::requestInventory
        ));
    }

    // ------------------------------------------------------------------
    // Tool implementations
    // ------------------------------------------------------------------

    private Object listInventory(JsonNode args, User user) {
        String query = textOrNull(args, "query");
        String categoryName = textOrNull(args, "categoryName");
        boolean onlyAvailable = args.hasNonNull("onlyAvailable") && args.get("onlyAvailable").asBoolean();

        List<Map<String,Object>> matches = inventoryService.findAll().stream()
                .filter(i -> query == null || containsIgnoreCase(i.getInventoryName(), query)
                        || containsIgnoreCase(i.getBrand(), query) || containsIgnoreCase(i.getSpecification(), query))
                .filter(i -> categoryName == null || containsIgnoreCase(i.getCategoryName(), categoryName))
                .filter(i -> !onlyAvailable || (i.getAvailableQuantity() != null && i.getAvailableQuantity() > 0))
                .map(this::inventorySummary)
                .collect(Collectors.toList());
        return capList(matches);
    }

    private Object scanBarcode(JsonNode args, User user) {
        String barcode = requireText(args, "barcode");
        Optional<InventoryUnitDTO> unit = inventoryService.tryFindUnitByBarcode(barcode);
        if (unit.isEmpty()) {
            return Map.of("found", false, "message", "No inventory unit found for barcode '" + barcode + "'.");
        }
        return unitSummary(unit.get());
    }

    private Object addInventory(JsonNode args, User user) {
        String categoryName = requireText(args, "categoryName");
        String itemName = requireText(args, "itemName");
        Integer quantity = requireInt(args, "quantity");
        if (quantity < 0) {
            throw new BadRequestException("Quantity cannot be negative.");
        }
        CategoryDTO category = resolveCategoryByName(categoryName);

        InventoryDTO dto = new InventoryDTO();
        dto.setCategoryId(category.getId());
        dto.setInventoryName(itemName);
        dto.setBrand(textOrNull(args, "brand"));
        dto.setSpecification(textOrNull(args, "specification"));
        dto.setDescription(textOrNull(args, "description"));
        dto.setCondition(textOrNull(args, "condition"));
        dto.setQuantity(quantity);

        InventoryDTO created = inventoryService.create(dto);
        return Map.of(
                "message", "Registered.",
                "baseCode", created.getBarcode(),
                "totalQuantity", created.getQuantity(),
                "availableQuantity", created.getAvailableQuantity()
        );
    }

    private Object addCategory(JsonNode args, User user) {
        String name = requireText(args, "name");
        String code = requireText(args, "code").trim().toUpperCase();
        if (!code.matches("^[A-Za-z0-9]{2,10}$")) {
            throw new BadRequestException("Category code must be 2-10 letters/numbers, no spaces.");
        }
        CategoryDTO dto = new CategoryDTO();
        dto.setName(name);
        dto.setCode(code);
        dto.setDescription(textOrNull(args, "description"));
        CategoryDTO created = categoryService.create(dto);
        return Map.of("message", "Category created.", "name", created.getName(), "code", created.getCode());
    }

    private Object allocateInventory(JsonNode args, User user) {
        String itemName = requireText(args, "itemName");
        String brand = textOrNull(args, "brand");
        String departmentName = requireText(args, "departmentName");
        Integer quantity = requireInt(args, "quantity");
        if (quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1.");
        }

        List<InventoryDTO> matches = inventoryService.findAll().stream()
                .filter(i -> containsIgnoreCase(i.getInventoryName(), itemName))
                .filter(i -> brand == null || containsIgnoreCase(i.getBrand(), brand))
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            throw new BadRequestException("No inventory item matching '" + itemName + "' found. Try list_inventory first.");
        }
        if (matches.size() > 1) {
            String options = matches.stream()
                    .map(i -> i.getInventoryName() + (i.getBrand() != null ? " (" + i.getBrand() + ")" : ""))
                    .collect(Collectors.joining("; "));
            throw new BadRequestException("Multiple items match '" + itemName + "' - please specify a brand. Matches: " + options);
        }
        InventoryDTO inventory = matches.get(0);

        UUID departmentId = departmentService.findAll().stream()
                .filter(d -> containsIgnoreCase(d.getDepartmentName(), departmentName))
                .findFirst()
                .map(DepartmentDTO::getId)
                .orElseThrow(() -> new BadRequestException("No department matching '" + departmentName + "' found."));

        AllocationRequest request = new AllocationRequest();
        request.setInventoryId(inventory.getId());
        request.setDepartmentId(departmentId);
        request.setQuantity(quantity);

        AllocationDTO created = allocationService.allocate(request);
        return Map.of(
                "message", "Allocated.",
                "item", created.getInventoryName(),
                "department", created.getDepartmentName(),
                "quantity", created.getQuantity()
        );
    }

    private Object getRequests(JsonNode args, User user) {
        String status = textOrNull(args, "status");
        List<InventoryRequestDTO> results = inventoryRequestService.getAllRequests(status);
        return capList(results.stream().map(this::requestSummary).collect(Collectors.toList()));
    }

    private Object approveRequest(JsonNode args, User user) {
        UUID id = requireUuid(args, "requestId");
        UpdateRequestStatusDTO dto = new UpdateRequestStatusDTO();
        dto.setStatus(RequestStatus.APPROVED);
        InventoryRequestDTO updated = inventoryRequestService.updateRequestStatus(id, dto, user);
        return Map.of("message", "Approved.", "item", updated.getItemName(), "department", updated.getDepartmentName());
    }

    private Object rejectRequest(JsonNode args, User user) {
        UUID id = requireUuid(args, "requestId");
        String reason = requireText(args, "reason");
        UpdateRequestStatusDTO dto = new UpdateRequestStatusDTO();
        dto.setStatus(RequestStatus.REJECTED);
        dto.setRejectionReason(reason);
        InventoryRequestDTO updated = inventoryRequestService.updateRequestStatus(id, dto, user);
        return Map.of("message", "Rejected.", "item", updated.getItemName(), "department", updated.getDepartmentName());
    }

    private Object requestInventory(JsonNode args, User user) {
        String categoryName = requireText(args, "categoryName");
        String itemName = requireText(args, "itemName");
        Integer quantity = requireInt(args, "quantity");
        String neededByRaw = requireText(args, "neededBy");
        if (quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1.");
        }
        LocalDate neededBy;
        try {
            neededBy = LocalDate.parse(neededByRaw.trim());
        } catch (Exception ex) {
            throw new BadRequestException("neededBy must be a valid date in YYYY-MM-DD format.");
        }
        if (neededBy.isBefore(LocalDate.now())) {
            throw new BadRequestException("neededBy cannot be in the past.");
        }
        CategoryDTO category = resolveCategoryByName(categoryName);

        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setCategoryId(category.getId());
        dto.setItemName(itemName);
        dto.setQuantity(quantity);
        dto.setNeededBy(neededBy);
        dto.setDescription(textOrNull(args, "description"));

        InventoryRequestDTO created = inventoryRequestService.createRequest(dto, user);
        return Map.of(
                "message", "Request submitted.",
                "item", created.getItemName(),
                "quantity", created.getQuantity(),
                "status", created.getStatus().name()
        );
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private CategoryDTO resolveCategoryByName(String name) {
        return categoryService.findAll().stream()
                .filter(c -> containsIgnoreCase(c.getName(), name) || containsIgnoreCase(c.getCode(), name))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "No category matching '" + name + "' found. Call get_categories to see what exists, "
                                + "or add_category to create it first."));
    }

    private UUID requireOwnDepartmentId(User user) {
        if (user.getDepartment() == null) {
            throw new BadRequestException("Your account isn't linked to a department.");
        }
        return user.getDepartment().getId();
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private <T> List<T> capList(List<T> list) {
        return list.size() > MAX_RESULTS ? list.subList(0, MAX_RESULTS) : list;
    }

    private String textOrNull(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) return null;
        String value = node.asText().trim();
        return value.isEmpty() ? null : value;
    }

    private String requireText(JsonNode args, String field) {
        String value = textOrNull(args, field);
        if (value == null) {
            throw new BadRequestException("Missing required field: " + field);
        }
        return value;
    }

    private Integer requireInt(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull() || !node.canConvertToInt()) {
            throw new BadRequestException("Missing or invalid integer field: " + field);
        }
        return node.asInt();
    }

    private UUID requireUuid(JsonNode args, String field) {
        String text = requireText(args, field);
        try {
            return UUID.fromString(text.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Field '" + field + "' is not a valid id.");
        }
    }

    // --- compact summaries (keep tool results small/token-efficient) ---

    private Map<String, Object> inventorySummary(InventoryDTO i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("name", i.getInventoryName());
        m.put("brand", i.getBrand());
        m.put("specification", i.getSpecification());
        m.put("category", i.getCategoryName());
        m.put("baseCode", i.getBarcode());
        m.put("quantity", i.getQuantity());
        m.put("available", i.getAvailableQuantity());
        m.put("allocated", i.getAllocatedQuantity());
        m.put("status", i.getStatus());
        return m;
    }

    private Map<String, Object> categorySummary(CategoryDTO c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.getName());
        m.put("code", c.getCode());
        m.put("description", c.getDescription());
        return m;
    }

    private Map<String, Object> departmentSummary(DepartmentDTO d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", d.getDepartmentName());
        m.put("code", d.getDepartmentCode());
        return m;
    }

    private Map<String, Object> allocationSummary(AllocationDTO a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("item", a.getInventoryName());
        m.put("department", a.getDepartmentName());
        m.put("quantity", a.getQuantity());
        m.put("status", a.getStatus());
        m.put("allocatedAt", a.getAllocatedAt());
        return m;
    }

    private Map<String, Object> requestSummary(InventoryRequestDTO r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("department", r.getDepartmentName());
        m.put("category", r.getCategoryName());
        m.put("item", r.getItemName());
        m.put("quantity", r.getQuantity());
        m.put("neededBy", r.getNeededBy());
        m.put("status", r.getStatus());
        m.put("rejectionReason", r.getRejectionReason());
        return m;
    }

    private Map<String, Object> unitSummary(InventoryUnitDTO u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unitBarcode", u.getUnitBarcode());
        m.put("item", u.getInventoryName());
        m.put("category", u.getCategoryName());
        m.put("brand", u.getBrand());
        m.put("specification", u.getSpecification());
        m.put("condition", u.getCondition());
        m.put("status", u.getStatus());
        m.put("batchCode", u.getBatchCode());
        m.put("allocatedToDepartment", u.getDepartmentName());
        m.put("allocatedAt", u.getAllocatedAt());
        return m;
    }

    private Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        s.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            s.put("required", required);
        }
        return s;
    }

    private Map<String, Object> prop(String type, String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("description", description);
        return p;
    }
}
