package com.saas.procurement.review;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.review.ReviewDtos.ReactionForm;
import com.saas.procurement.review.ReviewDtos.ReviewForm;
import com.saas.procurement.review.ReviewDtos.ReviewList;
import com.saas.procurement.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService service;

	@GetMapping("/products/{productId}/reviews")
	public ReviewList list(@AuthenticationPrincipal AuthUser me, @PathVariable Long productId) {
		return service.list(me, productId);
	}

	/** Buyer who received the product: add or update my review. */
	@PreAuthorize("hasRole('BUYER')")
	@PostMapping("/products/{productId}/reviews")
	public ReviewList save(@AuthenticationPrincipal AuthUser me, @PathVariable Long productId,
			@Valid @RequestBody ReviewForm form) {
		return service.save(me, productId, form);
	}

	@DeleteMapping("/reviews/{id}")
	public ReviewList delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return service.delete(me, id);
	}

	@PutMapping("/reviews/{id}/reaction")
	public ReviewList react(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @RequestBody ReactionForm body) {
		return service.react(me, id, body.reaction());
	}

}
