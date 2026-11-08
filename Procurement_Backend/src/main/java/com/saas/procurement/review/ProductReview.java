package com.saas.procurement.review;

import com.saas.procurement.common.AuditedEntity;
import com.saas.procurement.order.PurchaseOrder;
import com.saas.procurement.product.Product;
import com.saas.procurement.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A buyer's 1-5 star rating and comment on a product they received. */
@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
public class ProductReview extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private PurchaseOrder order;

	@Column(nullable = false)
	private int rating;

	@Column(columnDefinition = "TEXT")
	private String comment;

}
