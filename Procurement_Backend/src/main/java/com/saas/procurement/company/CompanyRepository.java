package com.saas.procurement.company;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

	boolean existsByCountryCodeAndRegistrationNo(String countryCode, String registrationNo);

}
