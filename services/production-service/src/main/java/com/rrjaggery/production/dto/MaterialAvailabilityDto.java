package com.rrjaggery.production.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaterialAvailabilityDto {
    private UUID batchId;
    private String batchNumber;
    private boolean allMaterialsAvailable;
    private List<MaterialAvailabilityItemDto> materials = new ArrayList<>();

    public MaterialAvailabilityDto() {
    }

    public MaterialAvailabilityDto(UUID batchId, String batchNumber, boolean allMaterialsAvailable, List<MaterialAvailabilityItemDto> materials) {
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.allMaterialsAvailable = allMaterialsAvailable;
        this.materials = materials;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public boolean isAllMaterialsAvailable() {
        return allMaterialsAvailable;
    }

    public void setAllMaterialsAvailable(boolean allMaterialsAvailable) {
        this.allMaterialsAvailable = allMaterialsAvailable;
    }

    public List<MaterialAvailabilityItemDto> getMaterials() {
        return materials;
    }

    public void setMaterials(List<MaterialAvailabilityItemDto> materials) {
        this.materials = materials;
    }
}
