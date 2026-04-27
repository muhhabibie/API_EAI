package com.example.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CategoryRequest {
    
    @Schema(example = "Elektronik", description = "Nama kategori baru")
    private String name;

    // Getter and Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
