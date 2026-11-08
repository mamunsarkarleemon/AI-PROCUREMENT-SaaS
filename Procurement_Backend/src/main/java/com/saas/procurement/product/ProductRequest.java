package com.saas.procurement.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Create / update a product. */
public record ProductRequest(
		@NotBlank(message = "Product name is required") @Size(max = 200) String name,
		@Size(max = 5000) String description,
		Long categoryId,
		@NotBlank(message = "Unit is required") @Size(max = 30) String unit,
		@NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be more than 0") BigDecimal price,
		@NotNull(message = "Stock is required") @Min(value = 0, message = "Stock cannot be negative") Integer stockQty,
		@NotNull(message = "Minimum order is required") @Min(value = 1, message = "Minimum order must be at least 1") Integer minOrderQty,
		ProductStatus status) {
}
