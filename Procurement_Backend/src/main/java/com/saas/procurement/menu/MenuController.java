package com.saas.procurement.menu;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.menu.MenuService.MenuAccessRow;
import com.saas.procurement.menu.MenuService.MenuDto;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MenuController {

	private final MenuService menuService;

	/** Sidebar menus for the logged-in user. */
	@GetMapping("/me/menus")
	public List<MenuDto> myMenus(@AuthenticationPrincipal AuthUser me) {
		return menuService.menusFor(me.role());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin/menu-access")
	public List<MenuAccessRow> matrix() {
		return menuService.accessMatrix();
	}

	public record AccessChange(boolean enabled) {
	}

	/** Example: PUT /api/v1/admin/menu-access/negotiations/BUYER {"enabled": false} */
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/admin/menu-access/{menuKey}/{role}")
	public List<MenuAccessRow> change(@PathVariable String menuKey, @PathVariable Role role,
			@RequestBody AccessChange body) {
		return menuService.setAccess(menuKey, role, body.enabled());
	}

}
