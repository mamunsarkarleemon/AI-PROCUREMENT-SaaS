package com.saas.procurement.category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findAllByOrderByNameAsc();

	boolean existsByNameIgnoreCase(String name);

}
