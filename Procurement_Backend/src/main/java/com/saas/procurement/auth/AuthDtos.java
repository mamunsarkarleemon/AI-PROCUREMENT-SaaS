package com.saas.procurement.auth;

import com.saas.procurement.user.Role;
import com.saas.procurement.user.dto.UserDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() {
	}

	/** Sign-up form. role must be BUYER or SUPPLIER. */
	public record RegisterRequest(
			@NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
			@NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 200) String email,
			@NotBlank(message = "Password is required") @Size(min = 6, max = 72, message = "Password must be 6 to 72 characters") String password,
			@NotNull(message = "Choose Buyer or Supplier") Role role,
			@Size(max = 40) String phone,
			@NotBlank(message = "Company name is required") @Size(max = 200) String companyName,
			@Size(max = 100) String city,
			@Size(max = 100) String country) {
	}

	public record LoginRequest(
			@NotBlank(message = "Email is required") String email,
			@NotBlank(message = "Password is required") String password) {
	}

	public record LoginResponse(String token, UserDto user) {
	}

	public record ProfileUpdate(
			@NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
			@Size(max = 40) String phone,
			@Size(max = 200) String companyName,
			@Size(max = 300) String address,
			@Size(max = 100) String city,
			@Size(max = 100) String country,
			@Size(max = 3000) String about) {
	}

	public record PasswordChange(
			@NotBlank(message = "Current password is required") String currentPassword,
			@NotBlank @Size(min = 6, max = 72, message = "Password must be 6 to 72 characters") String newPassword) {
	}

}
