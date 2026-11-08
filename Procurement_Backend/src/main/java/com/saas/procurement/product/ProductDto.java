package com.saas.procurement.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.saas.procurement.category.CategoryController.CategoryDto;
import com.saas.procurement.media.MediaDto;
import com.saas.procurement.user.dto.PartyDto;

public record ProductDto(
		Long id,
		String name,
		String description,
		String unit,
		BigDecimal price,
		int stockQty,
		int minOrderQty,
		BigDecimal ratingAvg,
		int ratingCount,
		int soldQty,
		ProductStatus status,
		CategoryDto category,
		PartyDto supplier,
		String imageUrl,
		List<MediaDto> media,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static ProductDto from(Product p) {
		return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getUnit(), p.getPrice(), p.getStockQty(),
				p.getMinOrderQty(), p.getRatingAvg(), p.getRatingCount(), p.getSoldQty(), p.getStatus(), CategoryDto.from(p.getCategory()), PartyDto.from(p.getSupplier()),
				MediaDto.firstImage(p.getMedia()), MediaDto.list(p.getMedia()), p.getCreatedAt(), p.getUpdatedAt());
	}

}
