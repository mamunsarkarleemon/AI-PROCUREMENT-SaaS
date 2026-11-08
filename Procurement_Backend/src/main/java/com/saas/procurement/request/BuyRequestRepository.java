package com.saas.procurement.request;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.saas.procurement.user.UserStatus;

public interface BuyRequestRepository extends JpaRepository<BuyRequest, Long> {

	@Query("""
			select r from BuyRequest r join r.buyer b
			where r.status = :status and b.status = :buyerStatus
			  and (:categoryId is null or r.category.id = :categoryId)
			  and (lower(r.title) like :q or lower(coalesce(r.description, '')) like :q)
			order by r.createdAt desc
			""")
	List<BuyRequest> search(RequestStatus status, UserStatus buyerStatus, Long categoryId, String q);

	List<BuyRequest> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

	List<BuyRequest> findAllByOrderByCreatedAtDesc();

	long countByStatus(RequestStatus status);

	long countByBuyerIdAndStatus(Long buyerId, RequestStatus status);

}
