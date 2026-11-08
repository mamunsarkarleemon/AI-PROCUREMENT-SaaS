package com.saas.procurement.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.auth.AuthDtos.LoginRequest;
import com.saas.procurement.auth.AuthDtos.LoginResponse;
import com.saas.procurement.auth.AuthDtos.PasswordChange;
import com.saas.procurement.auth.AuthDtos.ProfileUpdate;
import com.saas.procurement.auth.AuthDtos.RegisterRequest;
import com.saas.procurement.common.ApiException;
import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.security.JwtService;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;
import com.saas.procurement.user.dto.UserDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final NotificationService notifications;

	@Transactional
	public UserDto register(RegisterRequest req) {
		if (req.role() == Role.ADMIN) {
			throw ApiException.badRequest("You can register as a Buyer or a Supplier.");
		}
		String email = normalizeEmail(req.email());
		if (userRepository.existsByEmail(email)) {
			throw ApiException.conflict("An account with this email already exists. Please log in.");
		}
		User u = new User();
		u.setFullName(req.fullName().trim());
		u.setEmail(email);
		u.setPasswordHash(passwordEncoder.encode(req.password()));
		u.setRole(req.role());
		u.setStatus(UserStatus.ACTIVE);
		u.setPhone(req.phone());
		u.setCompanyName(req.companyName().trim());
		u.setCity(req.city());
		u.setCountry(req.country());
		userRepository.save(u);

		notifications.notifyRole(Role.ADMIN, "New " + u.getRole().name().toLowerCase(Locale.ROOT) + " registered: "
				+ u.displayName() + " (" + u.getEmail() + ")", "/app/admin/users");
		return UserDto.from(u);
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest req) {
		User u = userRepository.findByEmail(normalizeEmail(req.email())).orElse(null);
		if (u == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password.");
		}
		if (u.getStatus() != UserStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Your account is inactive. Please contact the admin.");
		}
		return new LoginResponse(jwtService.createToken(u), UserDto.from(u));
	}

	@Transactional(readOnly = true)
	public UserDto me(Long userId) {
		return UserDto.from(find(userId));
	}

	@Transactional
	public UserDto updateProfile(Long userId, ProfileUpdate req) {
		User u = find(userId);
		u.setFullName(req.fullName().trim());
		u.setPhone(req.phone());
		u.setCompanyName(req.companyName());
		u.setAddress(req.address());
		u.setCity(req.city());
		u.setCountry(req.country());
		u.setAbout(req.about());
		return UserDto.from(u);
	}

	@Transactional
	public void changePassword(Long userId, PasswordChange req) {
		User u = find(userId);
		if (!passwordEncoder.matches(req.currentPassword(), u.getPasswordHash())) {
			throw ApiException.badRequest("Current password is wrong.");
		}
		u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
	}

	private User find(Long id) {
		return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User"));
	}

	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
