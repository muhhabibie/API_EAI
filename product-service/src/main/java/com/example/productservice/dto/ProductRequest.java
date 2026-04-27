package com.example.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ProductRequest {
    @Schema(example = "Laptop Gaming ASUS", description = "Nama produk")
    private String name;
    
    @Schema(example = "1", description = "ID Kategori produk (lihat daftar kategori)")
    private Long categoryId;
    
    @Schema(example = "15000000.0", description = "Harga produk")
    private Double price;
    
    @Schema(example = "10", description = "Stok awal produk")
    private Integer stock;
    
    @Schema(example = "Laptop spesifikasi tinggi untuk gaming dan desain", description = "Deskripsi produk")
    private String description;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
