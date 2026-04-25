package com.example.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProductRequest {
    @NotBlank(message = "Nama produk wajib diisi")
    @Size(min = 3, max = 100, message = "Nama produk minimal 3 dan maksimal 100 karakter")
    @Pattern(regexp = "^[a-zA-Z0-9 ]*$", message = "Nama produk hanya boleh berisi huruf, angka, dan spasi")
    private String name;

    @NotNull(message = "Harga wajib diisi")
    @Min(value = 1, message = "Harga minimal 1")
    private Double price;

    @NotNull(message = "Stok wajib diisi")
    @Min(value = 0, message = "Stok tidak boleh negatif")
    private Integer stock;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
