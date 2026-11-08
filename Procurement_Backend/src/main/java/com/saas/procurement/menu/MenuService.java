package com.saas.procurement.menu;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.user.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final MenuRepository menuRepository;
	private final RoleMenuAccessRepository accessRepository;

	public record MenuDto(String key, String label, String path, String icon) {
		static MenuDto from(Menu m) {
			return new MenuDto(m.getMenuKey(), m.getLabel(), m.getPath(), m.getIcon());
		}
	}

	/** One row of the admin "Menu Access" screen. */
	public record MenuAccessRow(String key, String label, String icon, boolean forBuyer, boolean forSupplier,
			boolean locked, boolean buyerEnabled, boolean supplierEnabled) {
	}

	/** The sidebar for a role, in display order. */
	@Transactional(readOnly = true)
	public List<MenuDto> menusFor(Role role) {
		List<Menu> all = menuRepository.findAllByOrderBySortOrderAsc();
		if (role == Role.ADMIN) {
			return all.stream().filter(Menu::isForAdmin).map(MenuDto::from).toList();
		}
		Set<Long> enabled = enabledMenuIds(role);
		return all.stream()
				.filter(m -> appliesTo(m, role))
				.filter(m -> m.isLocked() || enabled.contains(m.getId()))
				.map(MenuDto::from)
				.toList();
	}

	/** Used by the security layer to create MENU_xxx permissions. */
	@Transactional(readOnly = true)
	public Set<String> enabledMenuKeys(Role role) {
		return menusFor(role).stream().map(MenuDto::key).collect(Collectors.toSet());
	}

	@Transactional(readOnly = true)
	public List<MenuAccessRow> accessMatrix() {
		Set<Long> buyer = enabledMenuIds(Role.BUYER);
		Set<Long> supplier = enabledMenuIds(Role.SUPPLIER);
		return menuRepository.findAllByOrderBySortOrderAsc().stream()
				.filter(m -> m.isForBuyer() || m.isForSupplier())
				.map(m -> new MenuAccessRow(m.getMenuKey(), m.getLabel(), m.getIcon(), m.isForBuyer(),
						m.isForSupplier(), m.isLocked(),
						m.isForBuyer() && (m.isLocked() || buyer.contains(m.getId())),
						m.isForSupplier() && (m.isLocked() || supplier.contains(m.getId()))))
				.toList();
	}

	/** Admin switches one menu on or off for BUYER or SUPPLIER. */
	@Transactional
	public List<MenuAccessRow> setAccess(String menuKey, Role role, boolean enabled) {
		if (role == Role.ADMIN) {
			throw ApiException.badRequest("Admin menus cannot be changed.");
		}
		Menu menu = menuRepository.findByMenuKey(menuKey).orElseThrow(() -> ApiException.notFound("Menu"));
		if (!appliesTo(menu, role)) {
			throw ApiException.badRequest("'" + menu.getLabel() + "' is not available for " + role + ".");
		}
		if (menu.isLocked()) {
			throw ApiException.badRequest("'" + menu.getLabel() + "' is always visible.");
		}
		var existing = accessRepository.findByRoleAndMenuId(role, menu.getId());
		if (enabled && existing.isEmpty()) {
			accessRepository.save(new RoleMenuAccess(role, menu));
		}
		else if (!enabled) {
			existing.ifPresent(accessRepository::delete);
		}
		return accessMatrix();
	}

	private Set<Long> enabledMenuIds(Role role) {
		return accessRepository.findByRole(role).stream().map(a -> a.getMenu().getId()).collect(Collectors.toSet());
	}

	private static boolean appliesTo(Menu m, Role role) {
		return switch (role) {
			case ADMIN -> m.isForAdmin();
			case BUYER -> m.isForBuyer();
			case SUPPLIER -> m.isForSupplier();
		};
	}

}
