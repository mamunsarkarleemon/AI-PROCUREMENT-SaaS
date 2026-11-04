package com.saas.procurement.auth.dto;

import com.saas.procurement.company.CompanyType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public sign-up payload. Note what is NOT here: no role, no company id, no admin flag.
 * The server decides those (Appendix D: never accept a role from the request body).
 */
public record RegisterRequest(
		@NotBlank @Size(max = 150) String fullName,

		@NotBlank @Email @Size(max = 320) String email,

		// BCrypt only uses the first 72 bytes, so longer passwords are rejected instead of silently truncated.
		@NotBlank @Size(min = 8, max = 72)
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain at least one letter and one digit")
		String password,

		/** BUYER or SUPPLIER. Any other value (e.g. "ADMIN") is rejected with 400. */
		@NotNull CompanyType accountType,

		@NotBlank @Size(max = 200) String companyName,

		@NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a 2-letter ISO country code, e.g. QA")
		String countryCode,

		@Size(max = 100) String registrationNo) {

	/** Keeps the password out of logs if this object is ever printed. */
	@Override
	public String toString() {
		return "RegisterRequest[email=" + email + ", accountType=" + accountType + ", companyName=" + companyName
				+ ", countryCode=" + countryCode + ", password=***]";
	}

}
