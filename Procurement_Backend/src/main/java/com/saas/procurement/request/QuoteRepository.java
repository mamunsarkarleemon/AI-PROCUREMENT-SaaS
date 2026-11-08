package com.saas.procurement.request;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

	Optional<Quote> findByRequestIdAndSupplierId(Long requestId, Long supplierId);

	List<Quote> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

	long countBySupplierIdAndStatus(Long supplierId, QuoteStatus status);

}
