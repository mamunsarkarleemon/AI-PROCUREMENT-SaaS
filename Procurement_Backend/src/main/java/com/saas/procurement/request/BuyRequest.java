package com.saas.procurement.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saas.procurement.category.Category;
import com.saas.procurement.common.BaseEntity;
import com.saas.procurement.media.MediaFile;
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

/** "I want to buy ..." - posted by a buyer, visible to all suppliers. */
@Entity
@Table(name = "buy_requests")
@Getter
@Setter
@NoArgsConstructor
public class BuyRequest extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private String unit;

	/** Optional budget per unit. */
	@Column(name = "target_price", precision = 14, scale = 2)
	private BigDecimal targetPrice;

	@Column(name = "needed_by")
	private LocalDate neededBy;

	@Column(name = "delivery_city")
	private String deliveryCity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RequestStatus status = RequestStatus.OPEN;

	@OneToMany(mappedBy = "request")
	private List<MediaFile> media = new ArrayList<>();

	@OneToMany(mappedBy = "request")
	private List<Quote> quotes = new ArrayList<>();

}
