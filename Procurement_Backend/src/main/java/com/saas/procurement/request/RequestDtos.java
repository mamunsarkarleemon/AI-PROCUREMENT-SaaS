package com.saas.procurement.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.saas.procurement.category.CategoryController.CategoryDto;
import com.saas.procurement.common.Money;
import com.saas.procurement.media.MediaDto;
import com.saas.procurement.user.dto.PartyDto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response shapes for buying requests and quotes. */
public final class RequestDtos {

	private RequestDtos() {
	}

	public record BuyRequestForm(
			@NotBlank(message = "Title is required") @Size(max = 200) String title,
			@Size(max = 5000) String description,
			Long categoryId,
			@NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
			@NotBlank(message = "Unit is required") @Size(max = 30) String unit,
			@DecimalMin(value = "0.01", message = "Budget must be more than 0") BigDecimal targetPrice,
			LocalDate neededBy,
			@Size(max = 100) String deliveryCity) {
	}

	public record QuoteForm(
			@NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be more than 0") BigDecimal pricePerUnit,
			@NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
			@Min(value = 0, message = "Delivery days cannot be negative") Integer deliveryDays,
			Long productId,
			@Size(max = 3000) String message) {
	}

	public record QuoteDto(
			Long id,
			Long requestId,
			String requestTitle,
			String requestUnit,
			PartyDto supplier,
			PartyDto buyer,
			Long productId,
			String productName,
			BigDecimal pricePerUnit,
			int quantity,
			BigDecimal total,
			Integer deliveryDays,
			String message,
			QuoteStatus status,
			List<MediaDto> media,
			LocalDateTime createdAt) {

		public static QuoteDto from(Quote q) {
			BuyRequest r = q.getRequest();
			return new QuoteDto(q.getId(), r.getId(), r.getTitle(), r.getUnit(), PartyDto.from(q.getSupplier()),
					PartyDto.from(r.getBuyer()), q.getProduct() == null ? null : q.getProduct().getId(),
					q.getProduct() == null ? null : q.getProduct().getName(), q.getPricePerUnit(), q.getQuantity(),
					Money.total(q.getPricePerUnit(), q.getQuantity()), q.getDeliveryDays(), q.getMessage(),
					q.getStatus(), MediaDto.list(q.getMedia()), q.getCreatedAt());
		}
	}

	public record BuyRequestDto(
			Long id,
			String title,
			String description,
			int quantity,
			String unit,
			BigDecimal targetPrice,
			LocalDate neededBy,
			String deliveryCity,
			RequestStatus status,
			CategoryDto category,
			PartyDto buyer,
			String imageUrl,
			List<MediaDto> media,
			int quoteCount,
			/** Quotes the viewer may see: all for the owner/admin, only their own for a supplier. */
			List<QuoteDto> quotes,
			LocalDateTime createdAt) {

		public static BuyRequestDto from(BuyRequest r, List<Quote> visibleQuotes) {
			List<Quote> live = r.getQuotes().stream().filter(q -> q.getStatus() != QuoteStatus.WITHDRAWN).toList();
			return new BuyRequestDto(r.getId(), r.getTitle(), r.getDescription(), r.getQuantity(), r.getUnit(),
					r.getTargetPrice(), r.getNeededBy(), r.getDeliveryCity(), r.getStatus(),
					CategoryDto.from(r.getCategory()), PartyDto.from(r.getBuyer()), MediaDto.firstImage(r.getMedia()),
					MediaDto.list(r.getMedia()), live.size(),
					visibleQuotes.stream().sorted(Comparator.comparing(Quote::getPricePerUnit)).map(QuoteDto::from)
							.toList(),
					r.getCreatedAt());
		}
	}

}
