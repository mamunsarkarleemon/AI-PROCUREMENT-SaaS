package com.saas.procurement.auth;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.auth.dto.LoginRequest;
import com.saas.procurement.auth.dto.LoginResponse;
import com.saas.procurement.auth.dto.RegisterRequest;
import com.saas.procurement.common.error.ApiException;
import com.saas.procurement.common.error.ErrorCode;
import com.saas.procurement.company.Company;
import com.saas.procurement.company.CompanyMember;
import com.saas.procurement.company.CompanyMemberRepository;
import com.saas.procurement.company.CompanyRepository;
import com.saas.procurement.company.CompanyRole;
import com.saas.procurement.company.CompanyStatus;
import com.saas.procurement.company.CompanyType;
import com.saas.procurement.company.MemberStatus;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserProfileService;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;
import com.saas.procurement.user.dto.UserResponse;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final CompanyMemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService tokenService;
	private final UserProfileService profileService;

	/**
	 * Compared against when the email does not exist, so a login for an unknown email takes about
	 * as long as one with a wrong password (reduces account enumeration by timing).
	 */
	private final String dummyPasswordHash;

	public AuthService(UserRepository userRepository, CompanyRepository companyRepository,
			CompanyMemberRepository memberRepository, PasswordEncoder passwordEncoder, JwtTokenService tokenService,
			UserProfileService profileService) {
		this.userRepository = userRepository;
		this.companyRepository = companyRepository;
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.profileService = profileService;
		this.dummyPasswordHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
	}

	/**
	 * Creates the user, their company and an OWNER membership in one transaction.
	 * The platform role is always USER; the company role is derived from the account type.
	 */
	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		String countryCode = request.countryCode().trim().toUpperCase(Locale.ROOT);
		String registrationNo = blankToNull(request.registrationNo());

		if (userRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}
		if (registrationNo != null && companyRepository.existsByCountryCodeAndRegistrationNo(countryCode, registrationNo)) {
			throw new ApiException(ErrorCode.COMPANY_ALREADY_REGISTERED);
		}

		// Email verification arrives on Day 12; until then accounts are ACTIVE immediately.
		User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()),
				request.fullName().trim(), UserStatus.ACTIVE));

		CompanyType type = request.accountType();
		// Suppliers wait for admin review before they appear in search; buyers can start right away.
		CompanyStatus companyStatus = type == CompanyType.SUPPLIER ? CompanyStatus.PENDING_REVIEW : CompanyStatus.ACTIVE;
		Company company = companyRepository.save(
				new Company(request.companyName().trim(), type, countryCode, registrationNo, companyStatus));

		CompanyMember membership = memberRepository.save(
				new CompanyMember(company, user, CompanyRole.ownerOf(type), MemberStatus.ACTIVE));
		// Write the inserts now so DB-generated values (createdAt) are in the response
		// and constraint violations surface inside this method.
		memberRepository.flush();

		log.info("Registered user {} as {} of company {}", user.getId(), membership.getRole(), company.getId());
		return profileService.toResponse(user, List.of(membership));
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository.findByEmail(email).orElse(null);

		if (user == null) {
			passwordEncoder.matches(request.password(), dummyPasswordHash);
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			log.info("Failed login for user {}", user.getId());
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
		}
		// Status is only revealed after the correct password, so it cannot be used to probe accounts.
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
		}

		user.setLastLoginAt(Instant.now());
		List<CompanyMember> memberships = profileService.activeMemberships(user.getId());
		JwtTokenService.IssuedToken token = tokenService.issue(user, memberships);

		return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(),
				profileService.toResponse(user, memberships));
	}

	static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

}
