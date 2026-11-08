package com.saas.procurement.user.dto;

import com.saas.procurement.user.User;

/** Short public info about a buyer or supplier, shown on products, orders, quotes. */
public record PartyDto(Long id, String fullName, String companyName, String phone, String email, String city,
		String country) {

	public static PartyDto from(User u) {
		return u == null ? null
				: new PartyDto(u.getId(), u.getFullName(), u.displayName(), u.getPhone(), u.getEmail(), u.getCity(),
						u.getCountry());
	}

}
