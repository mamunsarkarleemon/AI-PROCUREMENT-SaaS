package com.saas.procurement.menu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saas.procurement.user.Role;

public interface RoleMenuAccessRepository extends JpaRepository<RoleMenuAccess, Long> {

	List<RoleMenuAccess> findByRole(Role role);

	Optional<RoleMenuAccess> findByRoleAndMenuId(Role role, Long menuId);

}
