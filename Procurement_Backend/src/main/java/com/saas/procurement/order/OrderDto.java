package com.saas.procurement.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.saas.procurement.media.MediaDto;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.dto.PartyDto;

public record OrderDto(
		Long id,
		String orderNo,
		OrderSource source,
		OrderStatus status,
		Long productId,
		String productName,
		String productImage,
		String unit,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal totalAmount,
		String note,
		PartyDto buyer,
		PartyDto supplier,
		Long requestId,
		Long quoteId,
		Long negotiationId,
		/** Statuses the current user is allowed to move this order to (drives the buttons). */
		List<OrderStatus> nextStatuses,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static OrderDto from(PurchaseOrder o, Role viewerRole) {
		var product = o.getProduct();
		return new OrderDto(o.getId(), o.orderNo(), o.getSource(), o.getStatus(),
				product == null ? null : product.getId(), o.getProductName(),
				product == null ? null : MediaDto.firstImage(product.getMedia()), o.getUnit(), o.getQuantity(),
				o.getUnitPrice(), o.getTotalAmount(), o.getNote(), PartyDto.from(o.getBuyer()),
				PartyDto.from(o.getSupplier()), o.getRequest() == null ? null : o.getRequest().getId(),
				o.getQuote() == null ? null : o.getQuote().getId(),
				o.getNegotiation() == null ? null : o.getNegotiation().getId(),
				OrderWorkflow.nextStatuses(o.getStatus(), viewerRole), o.getCreatedAt(), o.getUpdatedAt());
	}

}
