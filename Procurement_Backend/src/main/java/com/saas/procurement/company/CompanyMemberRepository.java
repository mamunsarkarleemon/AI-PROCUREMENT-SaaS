package com.saas.procurement.company;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {

	/** Memberships of one user, with the company loaded in the same query. */
	@EntityGraph(attributePaths = "company")
	List<CompanyMember> findByUserIdAndStatus(UUID userId, MemberStatus status);

}
