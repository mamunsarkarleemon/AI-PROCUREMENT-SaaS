package com.saas.procurement.user.dto;

import java.time.LocalDateTime;

import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserStatus;

/** Full user details (never includes the password hash). */
public record UserDto(
		Long id,
		String fullName,
		String email,
		Role role,
		UserStatus status,
		String phone,
		String companyName,
		String address,
		String city,
		String country,
		String about,
		LocalDateTime createdAt) {

	public static UserDto from(User u) {
		return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getStatus(), u.getPhone(),
				u.getCompanyName(), u.getAddress(), u.getCity(), u.getCountry(), u.getAbout(), u.getCreatedAt());
	}

}
