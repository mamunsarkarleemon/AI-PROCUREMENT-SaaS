package com.saas.procurement.negotiation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NegotiationRepository extends JpaRepository<Negotiation, Long> {

	List<Negotiation> findByBuyerIdOrderByUpdatedAtDesc(Long buyerId);

	List<Negotiation> findBySupplierIdOrderByUpdatedAtDesc(Long supplierId);

	List<Negotiation> findAllByOrderByUpdatedAtDesc();

	Optional<Negotiation> findFirstByProductIdAndBuyerIdAndStatus(Long productId, Long buyerId, Negotiation.Status status);

	long countByBuyerIdAndStatus(Long buyerId, Negotiation.Status status);

	long countBySupplierIdAndStatus(Long supplierId, Negotiation.Status status);

}
