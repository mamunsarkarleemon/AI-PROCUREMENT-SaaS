package com.saas.procurement.company;

/** Role of a user inside one company (blueprint 7.2). */
public enum CompanyRole {
	BUYER_OWNER,
	BUYER_MEMBER,
	SUPPLIER_OWNER,
	SUPPLIER_MEMBER;

	public static CompanyRole ownerOf(CompanyType type) {
		return type == CompanyType.BUYER ? BUYER_OWNER : SUPPLIER_OWNER;
	}

	/** The coarse role used by hasRole("BUYER") / hasRole("SUPPLIER") checks. */
	public CompanyType companyType() {
		return (this == BUYER_OWNER || this == BUYER_MEMBER) ? CompanyType.BUYER : CompanyType.SUPPLIER;
	}
}
