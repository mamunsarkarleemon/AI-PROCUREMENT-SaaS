package com.saas.procurement.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.saas.procurement.user.PlatformRole;
import com.saas.procurement.user.UserStatus;

/** Public view of a user. Never includes the password hash. */
public record UserResponse(
		UUID id,
		String email,
		String fullName,
		PlatformRole platformRole,
		UserStatus status,
		boolean emailVerified,
		Instant createdAt,
		List<MembershipResponse> memberships) {
}
