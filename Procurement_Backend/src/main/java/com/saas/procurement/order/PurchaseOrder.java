package com.saas.procurement.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.saas.procurement.common.AuditedEntity;
import com.saas.procurement.negotiation.Negotiation;
import com.saas.procurement.product.Product;
import com.saas.procurement.request.BuyRequest;
import com.saas.procurement.request.Quote;
import com.saas.procurement.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An order between one buyer and one supplier (table "orders"). */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "supplier_id", nullable = false)
	private User supplier;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "request_id")
	private BuyRequest request;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "quote_id")
	private Quote quote;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "negotiation_id")
	private Negotiation negotiation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderSource source;

	/** Copied from the product so the order stays correct if the product changes later. */
	@Column(name = "product_name", nullable = false)
	private String productName;

	@Column(nullable = false)
	private String unit;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
	private BigDecimal unitPrice;

	/** What the buyer pays: the full price, or only the received units after a partial delivery. */
	@Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status = OrderStatus.PENDING;

	@Column(columnDefinition = "TEXT")
	private String note;

	/** Good units the buyer accepted. Empty until the order is delivered. */
	@Column(name = "received_qty")
	private Integer receivedQty;

	/** Damaged units the buyer sent back. */
	@Column(name = "returned_qty", nullable = false)
	private int returnedQty;

	@Column(name = "return_reason", columnDefinition = "TEXT")
	private String returnReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "return_status", nullable = false)
	private ReturnStatus returnStatus = ReturnStatus.NONE;

	@Column(name = "return_confirmed_at")
	private LocalDateTime returnConfirmedAt;

	/** True when part of this order was sent back. Such an order is settled and its status stays fixed. */
	public boolean hasReturn() {
		return returnStatus != ReturnStatus.NONE;
	}

	/** Units that count for reports: what was received, or what was ordered if not delivered yet. */
	public int effectiveQty() {
		return receivedQty != null ? receivedQty : quantity;
	}

	/** Readable number shown in the UI, e.g. ORD-000012 */
	public String orderNo() {
		return String.format("ORD-%06d", getId());
	}

}
