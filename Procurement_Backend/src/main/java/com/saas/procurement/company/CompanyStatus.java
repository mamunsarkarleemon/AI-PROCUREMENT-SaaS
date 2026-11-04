package com.saas.procurement.company;

public enum CompanyStatus {
	ACTIVE,
	/** Suppliers start here until an admin reviews them (blueprint Day 27). */
	PENDING_REVIEW,
	SUSPENDED
}
