package com.saas.procurement.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.saas.procurement.auth.dto.LoginRequest;
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
import com.saas.procurement.user.PlatformRole;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserProfileService;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;
import com.saas.procurement.user.dto.UserResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final PasswordEncoder ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@Mock UserRepository userRepository;
	@Mock CompanyRepository companyRepository;
	@Mock CompanyMemberRepository memberRepository;
	@Mock JwtTokenService tokenService;

	AuthService authService;

	@BeforeEach
	void setUp() {
		UserProfileService profileService = new UserProfileService(userRepository, memberRepository);
		authService = new AuthService(userRepository, companyRepository, memberRepository, ENCODER, tokenService,
				profileService);
	}

	private static RegisterRequest request(CompanyType type) {
		return new RegisterRequest("Aisha Rahman", "  Aisha@Example.COM ", "Secret123", type, "Doha Foods",
				"qa", null);
	}

	private void stubSaves() {
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));
		when(memberRepository.save(any(CompanyMember.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@Test
	void register_hashesPassword_normalizesEmail_andAssignsOwnerRole() {
		stubSaves();

		UserResponse response = authService.register(request(CompanyType.BUYER));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		User user = saved.getValue();
		assertThat(user.getEmail()).isEqualTo("aisha@example.com");
		assertThat(user.getPasswordHash()).startsWith("{bcrypt}").doesNotContain("Secret123");
		assertThat(ENCODER.matches("Secret123", user.getPasswordHash())).isTrue();
		assertThat(user.getPlatformRole()).isEqualTo(PlatformRole.USER);

		assertThat(response.memberships()).singleElement().satisfies(m -> {
			assertThat(m.role()).isEqualTo(CompanyRole.BUYER_OWNER);
			assertThat(m.companyStatus()).isEqualTo(CompanyStatus.ACTIVE);
			assertThat(m.countryCode()).isEqualTo("QA");
		});
	}

	@Test
	void register_supplierCompanyStartsPendingReview() {
		stubSaves();

		UserResponse response = authService.register(request(CompanyType.SUPPLIER));

		assertThat(response.memberships()).singleElement().satisfies(m -> {
			assertThat(m.role()).isEqualTo(CompanyRole.SUPPLIER_OWNER);
			assertThat(m.companyStatus()).isEqualTo(CompanyStatus.PENDING_REVIEW);
		});
	}

	@Test
	void register_rejectsDuplicateEmail() {
		when(userRepository.existsByEmail("aisha@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request(CompanyType.BUYER)))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED));
		verify(userRepository, never()).save(any());
	}

	@Test
	void login_unknownEmail_isInvalidCredentials() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "Secret123")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
	}

	@Test
	void login_wrongPassword_isInvalidCredentials_sameAsUnknownEmail() {
		User user = existingUser(UserStatus.ACTIVE);
		when(userRepository.findByEmail("aisha@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(new LoginRequest("aisha@example.com", "WrongPass1")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
		verify(tokenService, never()).issue(any(), any());
	}

	@Test
	void login_disabledUser_isRejected() {
		User user = existingUser(UserStatus.DISABLED);
		when(userRepository.findByEmail("aisha@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(new LoginRequest("aisha@example.com", "Secret123")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
	}

	@Test
	void login_success_issuesToken_andRecordsLastLogin() {
		User user = existingUser(UserStatus.ACTIVE);
		when(userRepository.findByEmail("aisha@example.com")).thenReturn(Optional.of(user));
		when(memberRepository.findByUserIdAndStatus(user.getId(), MemberStatus.ACTIVE)).thenReturn(List.of());
		when(tokenService.issue(any(), any())).thenReturn(new JwtTokenService.IssuedToken("jwt-value", 3600));

		var response = authService.login(new LoginRequest(" AISHA@example.com ", "Secret123"));

		assertThat(response.accessToken()).isEqualTo("jwt-value");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600);
		assertThat(user.getLastLoginAt()).isNotNull();
	}

	@Test
	void tokenRoles_neverIncludeAdminForRegisteredUsers() {
		User user = existingUser(UserStatus.ACTIVE);
		Company company = new Company("Doha Foods", CompanyType.BUYER, "QA", null, CompanyStatus.ACTIVE);
		CompanyMember member = new CompanyMember(company, user, CompanyRole.BUYER_OWNER, MemberStatus.ACTIVE);

		assertThat(JwtTokenService.rolesOf(user, List.of(member)))
				.containsExactly("USER", "BUYER", "BUYER_OWNER")
				.doesNotContain("PLATFORM_ADMIN", "ADMIN");
	}

	private static User existingUser(UserStatus status) {
		User user = new User("aisha@example.com", ENCODER.encode("Secret123"), "Aisha Rahman", status);
		user.setId(UUID.randomUUID());
		return user;
	}

}
