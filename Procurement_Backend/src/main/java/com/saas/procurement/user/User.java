package com.saas.procurement.user;

import com.saas.procurement.common.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A buyer, supplier or admin. Company details live on the same row to keep things simple. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends AuditedEntity {

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String email;

	/** BCrypt hash. The real password is never stored. */
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status = UserStatus.ACTIVE;

	private String phone;

	@Column(name = "company_name")
	private String companyName;

	private String address;

	private String city;

	private String country;

	@Column(columnDefinition = "TEXT")
	private String about;

	/** Company name if set, otherwise the person's name. */
	public String displayName() {
		return (companyName != null && !companyName.isBlank()) ? companyName : fullName;
	}

}
