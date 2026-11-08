package com.saas.procurement.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {

	List<PurchaseOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

	List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

	List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

	List<PurchaseOrder> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to);

	Optional<PurchaseOrder> findFirstByNegotiationId(Long negotiationId);

	Optional<PurchaseOrder> findFirstByQuoteId(Long quoteId);

	boolean existsByBuyerIdOrSupplierId(Long buyerId, Long supplierId);

	Optional<PurchaseOrder> findFirstByBuyerIdAndProductIdAndStatusOrderByCreatedAtDesc(Long buyerId, Long productId,
			OrderStatus status);

	/** Units of a product buyers have actually received. */
	@Query("""
			select coalesce(sum(o.receivedQty), 0L) from PurchaseOrder o
			where o.product.id = :productId and o.status = com.saas.procurement.order.OrderStatus.DELIVERED
			""")
	long sumReceivedQty(Long productId);

}
