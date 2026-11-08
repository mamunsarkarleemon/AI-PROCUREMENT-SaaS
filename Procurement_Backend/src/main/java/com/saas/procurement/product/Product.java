package com.saas.procurement.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.saas.procurement.category.Category;
import com.saas.procurement.common.AuditedEntity;
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

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "supplier_id", nullable = false)
	private User supplier;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	/** pcs, kg, box, carton, litre ... */
	@Column(nullable = false)
	private String unit;

	/** Price per unit. */
	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal price;

	@Column(name = "stock_qty", nullable = false)
	private int stockQty;

	/** Minimum order quantity (MOQ). */
	@Column(name = "min_order_qty", nullable = false)
	private int minOrderQty = 1;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductStatus status = ProductStatus.ACTIVE;

	/** Average star rating, 0 when there are no reviews. Kept up to date by ReviewService. */
	@Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
	private BigDecimal ratingAvg = BigDecimal.ZERO;

	@Column(name = "rating_count", nullable = false)
	private int ratingCount;

	/** Units actually received by buyers. Kept up to date by OrderService. */
	@Column(name = "sold_qty", nullable = false)
	private int soldQty;

	@OneToMany(mappedBy = "product")
	private List<MediaFile> media = new ArrayList<>();

}
