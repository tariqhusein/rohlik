package com.rohlikgroup.casestudy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        
        @NotBlank(message = "Product name is required")
        String name,
        
        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price must be greater than or equal to 0")
        BigDecimal price,
        
        @NotNull(message = "Quantity in stock is required")
        @PositiveOrZero(message = "Quantity in stock must be greater than or equal to 0")
        Integer quantityInStock
) {}
