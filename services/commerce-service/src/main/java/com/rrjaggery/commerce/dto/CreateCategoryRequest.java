package com.rrjaggery.commerce.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String slug;
    private String description;
    private int displayOrder = 0;

    public CreateCategoryRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
