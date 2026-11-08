package com.saas.procurement.negotiation;

import java.math.BigDecimal;

import com.saas.procurement.common.BaseEntity;
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

/** One step in the conversation: an offer, a counter-offer, an accept or a reject. */
@Entity
@Table(name = "negotiation_messages")
@Getter
@Setter
@NoArgsConstructor
public class NegotiationMessage extends BaseEntity {

	public enum Action {
		OFFER, COUNTER, ACCEPT, REJECT
	}

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "negotiation_id", nullable = false)
	private Negotiation negotiation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sender_id", nullable = false)
	private User sender;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Action action;

	@Column(name = "price_per_unit", precision = 14, scale = 2)
	private BigDecimal pricePerUnit;

	private Integer quantity;

	@Column(columnDefinition = "TEXT")
	private String message;

}
