package com.saas.procurement.menu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

	List<Menu> findAllByOrderBySortOrderAsc();

	Optional<Menu> findByMenuKey(String menuKey);

}
