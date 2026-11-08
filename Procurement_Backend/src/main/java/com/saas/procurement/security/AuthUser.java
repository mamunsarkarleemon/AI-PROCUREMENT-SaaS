package com.saas.procurement.security;

import com.saas.procurement.user.Role;

/**
 * The logged-in user, available in controllers with {@code @AuthenticationPrincipal AuthUser me}.
 * Loaded from the database on every request, so role/status changes by an admin apply immediately.
 */
public record AuthUser(Long id, String email, String fullName, Role role) {

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	public boolean isBuyer() {
		return role == Role.BUYER;
	}

	public boolean isSupplier() {
		return role == Role.SUPPLIER;
	}

}
