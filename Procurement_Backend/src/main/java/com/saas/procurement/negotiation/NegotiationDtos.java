package com.saas.procurement.negotiation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.saas.procurement.common.Money;
import com.saas.procurement.media.MediaDto;
import com.saas.procurement.negotiation.NegotiationMessage.Action;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.dto.PartyDto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class NegotiationDtos {

	private NegotiationDtos() {
	}

	/** Buyer starts a negotiation on a product. */
	public record StartForm(
			@NotNull Long productId,
			@NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
			@NotNull(message = "Your price is required") @DecimalMin(value = "0.01", message = "Price must be more than 0") BigDecimal pricePerUnit,
			@Size(max = 2000) String message) {
	}

	/** Either side proposes a new price / quantity. */
	public record CounterForm(
			@NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
			@NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be more than 0") BigDecimal pricePerUnit,
			@Size(max = 2000) String message) {
	}

	public record ReplyForm(@Size(max = 2000) String message) {
	}

	public record MessageDto(Long id, Long senderId, String senderName, Role senderRole, Action action,
			BigDecimal pricePerUnit, Integer quantity, BigDecimal total, String message, LocalDateTime createdAt) {

		static MessageDto from(NegotiationMessage m) {
			var s = m.getSender();
			return new MessageDto(m.getId(), s.getId(), s.displayName(), s.getRole(), m.getAction(), m.getPricePerUnit(),
					m.getQuantity(),
					m.getPricePerUnit() == null || m.getQuantity() == null ? null
							: Money.total(m.getPricePerUnit(), m.getQuantity()),
					m.getMessage(), m.getCreatedAt());
		}
	}

	public record NegotiationDto(
			Long id,
			Long productId,
			String productName,
			String productImage,
			String unit,
			BigDecimal listPrice,
			int stockQty,
			int minOrderQty,
			PartyDto buyer,
			PartyDto supplier,
			BigDecimal currentPrice,
			int currentQty,
			BigDecimal currentTotal,
			Role lastOfferBy,
			Negotiation.Status status,
			/** True when the viewer must answer (accept / counter / reject). */
			boolean myTurn,
			Long orderId,
			List<MessageDto> messages,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {

		static NegotiationDto from(Negotiation n, AuthUser me, Long orderId, boolean withMessages) {
			var p = n.getProduct();
			boolean myTurn = n.getStatus() == Negotiation.Status.OPEN && me.role() != Role.ADMIN
					&& n.getLastOfferBy() != me.role();
			return new NegotiationDto(n.getId(), p.getId(), p.getName(), MediaDto.firstImage(p.getMedia()), p.getUnit(),
					p.getPrice(), p.getStockQty(), p.getMinOrderQty(), PartyDto.from(n.getBuyer()),
					PartyDto.from(n.getSupplier()), n.getCurrentPrice(), n.getCurrentQty(),
					Money.total(n.getCurrentPrice(), n.getCurrentQty()), n.getLastOfferBy(), n.getStatus(), myTurn,
					orderId, withMessages ? n.getMessages().stream().map(MessageDto::from).toList() : List.of(),
					n.getCreatedAt(), n.getUpdatedAt());
		}
	}

}
