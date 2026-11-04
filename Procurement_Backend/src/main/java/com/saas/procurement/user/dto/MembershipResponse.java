package com.saas.procurement.user.dto;

import java.util.UUID;

import com.saas.procurement.company.CompanyMember;
import com.saas.procurement.company.CompanyRole;
import com.saas.procurement.company.CompanyStatus;
import com.saas.procurement.company.CompanyType;

public record MembershipResponse(
		UUID companyId,
		String companyName,
		CompanyType companyType,
		CompanyStatus companyStatus,
		String countryCode,
		CompanyRole role) {

	public static MembershipResponse from(CompanyMember member) {
		var company = member.getCompany();
		return new MembershipResponse(company.getId(), company.getDisplayName(), company.getCompanyType(),
				company.getStatus(), company.getCountryCode(), member.getRole());
	}

}
