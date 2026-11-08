package com.saas.procurement.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.saas.procurement.auth.AuthDtos.LoginRequest;
import com.saas.procurement.auth.AuthDtos.RegisterRequest;
import com.saas.procurement.common.ApiException;
import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.security.JwtService;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

	@Mock UserRepository userRepository;
	@Mock JwtService jwtService;
	@Mock NotificationService notifications;

	AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(userRepository, ENCODER, jwtService, notifications);
	}

	private static RegisterRequest register(Role role) {
		return new RegisterRequest("Aisha Rahman", " Aisha@Example.com ", "secret1", role, "+974", "Doha Foods",
				"Doha", "Qatar");
	}

	@Test
	void register_hashesPassword_andLowercasesEmail() {
		when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

		var dto = service.register(register(Role.SUPPLIER));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("aisha@example.com");
		assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret1");
		assertThat(ENCODER.matches("secret1", saved.getValue().getPasswordHash())).isTrue();
		assertThat(dto.role()).isEqualTo(Role.SUPPLIER);
	}

	@Test
	void register_cannotCreateAdmin() {
		assertThatThrownBy(() -> service.register(register(Role.ADMIN))).isInstanceOf(ApiException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void register_duplicateEmail_isConflict() {
		when(userRepository.existsByEmail("aisha@example.com")).thenReturn(true);
		assertThatThrownBy(() -> service.register(register(Role.BUYER)))
				.isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void login_wrongPassword_isUnauthorized() {
		when(userRepository.findByEmail("aisha@example.com")).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
		assertThatThrownBy(() -> service.login(new LoginRequest("aisha@example.com", "wrong")))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void login_inactiveUser_isForbidden() {
		when(userRepository.findByEmail("aisha@example.com")).thenReturn(Optional.of(user(UserStatus.INACTIVE)));
		assertThatThrownBy(() -> service.login(new LoginRequest("aisha@example.com", "secret1")))
				.isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void login_success_returnsToken() {
		User u = user(UserStatus.ACTIVE);
		when(userRepository.findByEmail("aisha@example.com")).thenReturn(Optional.of(u));
		when(jwtService.createToken(u)).thenReturn("token-123");

		var res = service.login(new LoginRequest(" AISHA@example.com", "secret1"));

		assertThat(res.token()).isEqualTo("token-123");
		assertThat(res.user().email()).isEqualTo("aisha@example.com");
	}

	private static User user(UserStatus status) {
		User u = new User();
		u.setId(1L);
		u.setEmail("aisha@example.com");
		u.setFullName("Aisha Rahman");
		u.setPasswordHash(ENCODER.encode("secret1"));
		u.setRole(Role.BUYER);
		u.setStatus(status);
		return u;
	}

}
