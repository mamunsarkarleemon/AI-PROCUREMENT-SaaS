package com.saas.procurement.company;

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
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "legal_name", nullable = false, length = 200)
	private String legalName;

	@Column(name = "display_name", length = 200)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(name = "company_type", nullable = false, length = 20)
	private CompanyType companyType;

	/** ISO 3166-1 alpha-2, e.g. QA. */
	@Column(name = "country_code", nullable = false, length = 2)
	private String countryCode;

	@Column(name = "registration_no", length = 100)
	private String registrationNo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CompanyStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private long version;

	public Company(String legalName, CompanyType companyType, String countryCode, String registrationNo,
			CompanyStatus status) {
		this.legalName = legalName;
		this.displayName = legalName;
		this.companyType = companyType;
		this.countryCode = countryCode;
		this.registrationNo = registrationNo;
		this.status = status;
	}

}
