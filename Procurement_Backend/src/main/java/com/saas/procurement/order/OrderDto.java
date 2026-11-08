package com.saas.procurement.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.saas.procurement.common.Money;
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
		/** Price of everything ordered; differs from totalAmount after a partial delivery. */
		BigDecimal orderedAmount,
		Integer receivedQty,
		int returnedQty,
		String returnReason,
		ReturnStatus returnStatus,
		LocalDateTime returnConfirmedAt,
		/** True when the current user is the one who must confirm the returned units came back. */
		boolean canConfirmReturn,
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
				o.getUnitPrice(), o.getTotalAmount(), Money.total(o.getUnitPrice(), o.getQuantity()), o.getReceivedQty(),
				o.getReturnedQty(), o.getReturnReason(), o.getReturnStatus(), o.getReturnConfirmedAt(),
				o.getReturnStatus() == ReturnStatus.PENDING && viewerRole != Role.BUYER, o.getNote(),
				PartyDto.from(o.getBuyer()),
				PartyDto.from(o.getSupplier()), o.getRequest() == null ? null : o.getRequest().getId(),
				o.getQuote() == null ? null : o.getQuote().getId(),
				o.getNegotiation() == null ? null : o.getNegotiation().getId(),
				o.hasReturn() ? List.of() : OrderWorkflow.nextStatuses(o.getStatus(), viewerRole), o.getCreatedAt(),
				o.getUpdatedAt());
	}

}
