package com.saas.procurement.review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.order.OrderRepository;
import com.saas.procurement.order.OrderStatus;
import com.saas.procurement.order.PurchaseOrder;
import com.saas.procurement.product.Product;
import com.saas.procurement.product.ProductService;
import com.saas.procurement.review.ReviewDtos.ReviewDto;
import com.saas.procurement.review.ReviewDtos.ReviewForm;
import com.saas.procurement.review.ReviewDtos.ReviewList;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Buyers rate products they received (one review per product, can be edited).
 * Every logged-in user can react to other people's reviews.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ProductReviewRepository repository;
	private final ReviewReactionRepository reactions;
	private final ProductService productService;
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final NotificationService notifications;

	@Transactional(readOnly = true)
	public ReviewList list(AuthUser me, Long productId) {
		productService.get(productId, me); // same visibility rules as the product itself
		return build(me, productId);
	}

	/** Creates my review of this product, or updates it if I already wrote one. */
	@Transactional
	public ReviewList save(AuthUser me, Long productId, ReviewForm form) {
		Product product = productService.find(productId);
		PurchaseOrder order = latestDeliveredOrder(me.id(), productId);
		if (order == null) {
			throw ApiException.badRequest("You can review a product after you have received it.");
		}
		ProductReview review = repository.findByProductIdAndBuyerId(productId, me.id()).orElse(null);
		boolean isNew = review == null;
		if (isNew) {
			review = new ProductReview();
			review.setProduct(product);
			review.setBuyer(userRepository.getReferenceById(me.id()));
		}
		review.setOrder(order);
		review.setRating(form.rating());
		review.setComment(form.comment() == null || form.comment().isBlank() ? null : form.comment().trim());
		repository.save(review);
		refreshRating(product);
		if (isNew) {
			notifications.notify(product.getSupplier(), "New " + form.rating() + "-star review on " + product.getName() + ".",
					"/app/products/" + productId + "#reviews");
		}
		return build(me, productId);
	}

	/** The author or an admin can remove a review. */
	@Transactional
	public ReviewList delete(AuthUser me, Long reviewId) {
		ProductReview review = find(reviewId);
		if (!me.isAdmin() && !review.getBuyer().getId().equals(me.id())) {
			throw ApiException.forbidden();
		}
		Product product = review.getProduct();
		repository.delete(review);
		refreshRating(product);
		return build(me, product.getId());
	}

	/** Sets, changes or (reaction = null) removes my reaction to someone else's review. */
	@Transactional
	public ReviewList react(AuthUser me, Long reviewId, ReactionType reaction) {
		ProductReview review = find(reviewId);
		Long productId = review.getProduct().getId();
		productService.get(productId, me);
		if (review.getBuyer().getId().equals(me.id())) {
			throw ApiException.badRequest("You cannot react to your own review.");
		}
		ReviewReaction existing = reactions.findByReviewIdAndUserId(reviewId, me.id()).orElse(null);
		if (reaction == null) {
			if (existing != null) {
				reactions.delete(existing);
			}
		}
		else if (existing != null) {
			existing.setReaction(reaction);
		}
		else {
			ReviewReaction created = new ReviewReaction();
			created.setReview(review);
			created.setUser(userRepository.getReferenceById(me.id()));
			created.setReaction(reaction);
			reactions.save(created);
		}
		return build(me, productId);
	}

	private ProductReview find(Long id) {
		return repository.findById(id).orElseThrow(() -> ApiException.notFound("Review"));
	}

	private PurchaseOrder latestDeliveredOrder(Long buyerId, Long productId) {
		return orderRepository
				.findFirstByBuyerIdAndProductIdAndStatusOrderByCreatedAtDesc(buyerId, productId, OrderStatus.DELIVERED)
				.orElse(null);
	}

	/** Recalculates the numbers shown on product cards from the reviews themselves. */
	private void refreshRating(Product product) {
		List<ProductReview> all = repository.findByProductIdOrderByCreatedAtDesc(product.getId());
		product.setRatingAvg(average(all));
		product.setRatingCount(all.size());
	}

	static BigDecimal average(List<ProductReview> reviews) {
		if (reviews.isEmpty()) {
			return BigDecimal.ZERO.setScale(2);
		}
		int sum = reviews.stream().mapToInt(ProductReview::getRating).sum();
		return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
	}

	private ReviewList build(AuthUser me, Long productId) {
		List<ProductReview> reviews = repository.findByProductIdOrderByCreatedAtDesc(productId);
		Map<Long, List<ReviewReaction>> byReview = reviews.isEmpty() ? Map.of()
				: reactions.findByReviewIdIn(reviews.stream().map(ProductReview::getId).toList()).stream()
						.collect(Collectors.groupingBy(r -> r.getReview().getId()));

		List<ReviewDto> items = reviews.stream().map(r -> toDto(r, byReview.getOrDefault(r.getId(), List.of()), me)).toList();

		Map<Integer, Long> breakdown = new LinkedHashMap<>();
		for (int stars = 5; stars >= 1; stars--) {
			int s = stars;
			breakdown.put(s, reviews.stream().filter(r -> r.getRating() == s).count());
		}
		boolean canReview = me.isBuyer() && latestDeliveredOrder(me.id(), productId) != null;
		ReviewDto mine = items.stream().filter(ReviewDto::mine).findFirst().orElse(null);
		return new ReviewList(average(reviews), reviews.size(), breakdown, canReview, mine, items);
	}

	private static ReviewDto toDto(ProductReview r, List<ReviewReaction> list, AuthUser me) {
		Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
		for (ReactionType type : ReactionType.values()) {
			counts.put(type, list.stream().filter(x -> x.getReaction() == type).count());
		}
		ReactionType my = list.stream().filter(x -> x.getUser().getId().equals(me.id()))
				.map(ReviewReaction::getReaction).findFirst().orElse(null);
		return new ReviewDto(r.getId(), r.getRating(), r.getComment(), r.getBuyer().displayName(),
				r.getBuyer().getId().equals(me.id()), counts, my, r.getCreatedAt(), r.getUpdatedAt());
	}

}
