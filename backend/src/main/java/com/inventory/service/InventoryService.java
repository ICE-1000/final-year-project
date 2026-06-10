package com.inventory.service;

import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryStatsDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Inventory;
import com.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryDTO> findAll() {
        return inventoryRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public InventoryDTO findByBarcode(String barcode) {
        return toDto(inventoryRepository.findByBarcodeAndDeletedFalse(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found")));
    }

    public InventoryStatsDTO stats() {
        return new InventoryStatsDTO(
                inventoryRepository.countByDeletedFalse(),
                inventoryRepository.countByDeletedFalseAndAvailableQuantityGreaterThan(0),
                inventoryRepository.countByDeletedFalseAndAllocatedQuantityGreaterThan(0),
                inventoryRepository.countByDeletedFalseAndAvailableQuantityLessThanEqual(5)
        );
    }

    @Transactional
    public InventoryDTO create(InventoryDTO dto) {
        validateQuantities(dto);
        Inventory inventory = new Inventory();
        apply(dto, inventory);
        inventory.setAvailableQuantity(dto.getAvailableQuantity() > 0 || dto.getAllocatedQuantity() > 0
                ? dto.getAvailableQuantity()
                : dto.getQuantity());
        inventory.setAllocatedQuantity(Math.max(dto.getAllocatedQuantity(), 0));
        inventory.setStatus(inventory.getAvailableQuantity() == 0 ? "ALLOCATED" : "AVAILABLE");
        return toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryDTO update(UUID id, InventoryDTO dto) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        validateQuantities(dto);
        apply(dto, inventory);
        inventory.setAvailableQuantity(dto.getAvailableQuantity());
        inventory.setAllocatedQuantity(dto.getAllocatedQuantity());
        inventory.setStatus(inventory.getAvailableQuantity() == 0 ? "ALLOCATED" : "AVAILABLE");
        return toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public void softDelete(UUID id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        inventory.setDeleted(true);
        inventoryRepository.save(inventory);
    }

    private void apply(InventoryDTO dto, Inventory inventory) {
        inventory.setBarcode(dto.getBarcode());
        inventory.setInventoryName(dto.getInventoryName());
        inventory.setCategory(dto.getCategory());
        inventory.setDescription(dto.getDescription());
        inventory.setQuantity(dto.getQuantity());
        inventory.setSerialNumber(dto.getSerialNumber());
        inventory.setCondition(dto.getCondition());
        inventory.setBarcodeImageUrl(dto.getBarcodeImageUrl());
    }

    private void validateQuantities(InventoryDTO dto) {
        if (dto.getQuantity() < 0 || dto.getAvailableQuantity() < 0 || dto.getAllocatedQuantity() < 0) {
            throw new BadRequestException("Quantities cannot be negative");
        }
        if (dto.getAvailableQuantity() + dto.getAllocatedQuantity() > dto.getQuantity()) {
            throw new BadRequestException("Available + allocated cannot exceed total quantity");
        }
    }

    public InventoryDTO toDto(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(inventory.getId());
        dto.setBarcode(inventory.getBarcode());
        dto.setInventoryName(inventory.getInventoryName());
        dto.setCategory(inventory.getCategory());
        dto.setDescription(inventory.getDescription());
        dto.setQuantity(inventory.getQuantity());
        dto.setAvailableQuantity(inventory.getAvailableQuantity());
        dto.setAllocatedQuantity(inventory.getAllocatedQuantity());
        dto.setSerialNumber(inventory.getSerialNumber());
        dto.setCondition(inventory.getCondition());
        dto.setStatus(inventory.getStatus());
        dto.setBarcodeImageUrl(inventory.getBarcodeImageUrl());
        dto.setCreatedAt(inventory.getCreatedAt());
        return dto;
    }
}
