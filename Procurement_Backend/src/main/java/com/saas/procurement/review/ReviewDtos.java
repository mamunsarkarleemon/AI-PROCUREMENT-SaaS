package com.saas.procurement.review;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReviewDtos {

	private ReviewDtos() {
	}

	public record ReviewForm(
			@NotNull(message = "Please choose a star rating") @Min(value = 1, message = "Rating must be 1 to 5") @Max(value = 5, message = "Rating must be 1 to 5") Integer rating,
			@Size(max = 2000) String comment) {
	}

	/** reaction = null removes my reaction. */
	public record ReactionForm(ReactionType reaction) {
	}

	public record ReviewDto(Long id, int rating, String comment, String reviewerName, boolean mine,
			Map<ReactionType, Long> reactions, ReactionType myReaction, LocalDateTime createdAt,
			LocalDateTime updatedAt) {
	}

	/** Everything the product page needs. canReview = this buyer has received the product. */
	public record ReviewList(BigDecimal average, int count, Map<Integer, Long> breakdown, boolean canReview,
			ReviewDto myReview, List<ReviewDto> items) {
	}

}
