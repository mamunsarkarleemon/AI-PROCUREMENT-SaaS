package com.saas.procurement.review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

	List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

	Optional<ProductReview> findByProductIdAndBuyerId(Long productId, Long buyerId);

}
