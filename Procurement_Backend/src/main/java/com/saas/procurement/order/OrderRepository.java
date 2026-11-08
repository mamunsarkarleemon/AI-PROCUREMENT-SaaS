package com.saas.procurement.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {

	List<PurchaseOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

	List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

	List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

	List<PurchaseOrder> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to);

	Optional<PurchaseOrder> findFirstByNegotiationId(Long negotiationId);

	Optional<PurchaseOrder> findFirstByQuoteId(Long quoteId);

	boolean existsByBuyerIdOrSupplierId(Long buyerId, Long supplierId);

}
