package com.saas.procurement.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByRole(Role role);

	long countByRole(Role role);

	List<User> findByRoleAndStatus(Role role, UserStatus status);

	List<User> findTop5ByOrderByCreatedAtDesc();

	@Query("""
			select u from User u
			where (:role is null or u.role = :role)
			  and (:status is null or u.status = :status)
			  and (lower(u.fullName) like :q or lower(u.email) like :q or lower(coalesce(u.companyName, '')) like :q)
			order by u.createdAt desc
			""")
	List<User> search(Role role, UserStatus status, String q);

}
