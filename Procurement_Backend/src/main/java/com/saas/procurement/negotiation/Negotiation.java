package com.saas.procurement.negotiation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.saas.procurement.common.AuditedEntity;
import com.saas.procurement.product.Product;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Price bargaining between one buyer and one supplier on one product.
 * The side that did NOT make the last offer can accept, counter or reject.
 */
@Entity
@Table(name = "negotiations")
@Getter
@Setter
@NoArgsConstructor
public class Negotiation extends AuditedEntity {

	public enum Status {
		OPEN, ACCEPTED, REJECTED
	}

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "supplier_id", nullable = false)
	private User supplier;

	@Column(name = "current_price", nullable = false, precision = 14, scale = 2)
	private BigDecimal currentPrice;

	@Column(name = "current_qty", nullable = false)
	private int currentQty;

	/** BUYER or SUPPLIER - who made the latest offer. */
	@Enumerated(EnumType.STRING)
	@Column(name = "last_offer_by", nullable = false)
	private Role lastOfferBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.OPEN;

	@OneToMany(mappedBy = "negotiation")
	@OrderBy("id ASC")
	private List<NegotiationMessage> messages = new ArrayList<>();

}
