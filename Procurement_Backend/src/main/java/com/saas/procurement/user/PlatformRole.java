package com.saas.procurement.user;

/**
 * Platform-wide role. Separate from company roles (see CompanyRole).
 * Public registration only ever assigns USER; PLATFORM_ADMIN is granted manually.
 */
public enum PlatformRole {
	USER,
	PLATFORM_ADMIN
}
