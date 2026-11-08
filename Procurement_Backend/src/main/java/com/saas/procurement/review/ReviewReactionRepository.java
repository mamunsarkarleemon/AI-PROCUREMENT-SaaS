package com.saas.procurement.review;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, Long> {

	List<ReviewReaction> findByReviewIdIn(Collection<Long> reviewIds);

	Optional<ReviewReaction> findByReviewIdAndUserId(Long reviewId, Long userId);

}
