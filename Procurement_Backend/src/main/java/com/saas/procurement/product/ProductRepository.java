package com.saas.procurement.product;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.saas.procurement.user.UserStatus;

public interface ProductRepository extends JpaRepository<Product, Long> {

	/** Marketplace search. q is a LIKE pattern such as "%box%" ("%%" = everything). */
	@Query("""
			select p from Product p join p.supplier s
			where p.status = :status and s.status = :supplierStatus
			  and (:categoryId is null or p.category.id = :categoryId)
			  and (lower(p.name) like :q or lower(coalesce(p.description, '')) like :q
			       or lower(coalesce(s.companyName, s.fullName)) like :q)
			order by p.createdAt desc
			""")
	List<Product> search(ProductStatus status, UserStatus supplierStatus, Long categoryId, String q);

	List<Product> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

	List<Product> findAllByOrderByCreatedAtDesc();

	List<Product> findByStatusAndSupplierStatusOrderByCreatedAtDesc(ProductStatus status, UserStatus supplierStatus,
			Limit limit);

	long countByStatus(ProductStatus status);

	long countBySupplierId(Long supplierId);

	List<Product> findTop5BySupplierIdAndStatusOrderByStockQtyAsc(Long supplierId, ProductStatus status);

}
