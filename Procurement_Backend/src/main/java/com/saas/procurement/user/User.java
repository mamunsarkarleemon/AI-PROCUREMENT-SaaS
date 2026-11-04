package com.saas.procurement.user;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Always stored lower-case (also enforced by a DB check constraint). */
	@Column(nullable = false, unique = true, length = 320)
	private String email;

	/** Encoded hash only - the raw password is never stored or logged. */
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(name = "platform_role", nullable = false, length = 30)
	private PlatformRole platformRole = PlatformRole.USER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private long version;

	public User(String email, String passwordHash, String fullName, UserStatus status) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.status = status;
		this.platformRole = PlatformRole.USER;
	}

}
