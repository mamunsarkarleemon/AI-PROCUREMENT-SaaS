package com.saas.procurement.media;

import com.saas.procurement.common.BaseEntity;
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

/** A photo or short video. Belongs to exactly one of: product, buy request or quote. */
@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
public class MediaFile extends BaseEntity {

	/** Public URL, e.g. /uploads/2026/11/3f2a....jpg */
	@Column(name = "file_url", nullable = false)
	private String fileUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "file_type", nullable = false)
	private FileType fileType;

	@Column(name = "original_name")
	private String originalName;

	@Column(name = "size_bytes")
	private Long sizeBytes;

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
	@JoinColumn(name = "uploaded_by")
	private User uploadedBy;

}
