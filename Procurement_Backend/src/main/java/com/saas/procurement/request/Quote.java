package com.saas.procurement.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.saas.procurement.common.BaseEntity;
import com.saas.procurement.media.MediaFile;
import com.saas.procurement.product.Product;
import com.saas.procurement.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A supplier's offer for a buyer's request: price, quantity, delivery time, photos/video. */
@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
public class Quote extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false)
	private BuyRequest request;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "supplier_id", nullable = false)
	private User supplier;

	/** Optional: one of the supplier's catalog products. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "price_per_unit", nullable = false, precision = 14, scale = 2)
	private BigDecimal pricePerUnit;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "delivery_days")
	private Integer deliveryDays;

	@Column(columnDefinition = "TEXT")
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QuoteStatus status = QuoteStatus.PENDING;

	@OneToMany(mappedBy = "quote")
	private List<MediaFile> media = new ArrayList<>();

}
