package com.saas.procurement.order;

import java.math.BigDecimal;

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

	@Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status = OrderStatus.PENDING;

	@Column(columnDefinition = "TEXT")
	private String note;

	/** Readable number shown in the UI, e.g. ORD-000012 */
	public String orderNo() {
		return String.format("ORD-%06d", getId());
	}

}
