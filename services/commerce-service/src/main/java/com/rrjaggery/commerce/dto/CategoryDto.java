package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.Category;
import java.util.UUID;

public class CategoryDto {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private int displayOrder;
    private boolean active;

    public CategoryDto() {}

    public static CategoryDto fromEntity(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.id = category.getId();
        dto.name = category.getName();
        dto.slug = category.getSlug();
        dto.description = category.getDescription();
        dto.displayOrder = category.getDisplayOrder();
        dto.active = category.isActive();
        return dto;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
}
