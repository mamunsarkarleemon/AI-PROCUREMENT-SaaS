package com.saas.procurement.admin;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.auth.AuthService;
import com.saas.procurement.common.ApiException;
import com.saas.procurement.order.OrderRepository;
import com.saas.procurement.product.ProductService;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;
import com.saas.procurement.user.dto.UserDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/** Admin: create, edit, activate/deactivate and delete any user. */
@Service
@RequiredArgsConstructor
public class AdminUserService {

	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final PasswordEncoder passwordEncoder;

	/** Used for create (password required) and update (password optional = keep current). */
	public record UserForm(
			@NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
			@NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 200) String email,
			@Size(max = 72, message = "Password must be at most 72 characters") String password,
			@NotNull(message = "Role is required") Role role,
			UserStatus status,
			@Size(max = 40) String phone,
			@Size(max = 200) String companyName,
			@Size(max = 300) String address,
			@Size(max = 100) String city,
			@Size(max = 100) String country,
			@Size(max = 3000) String about) {
	}

	@Transactional(readOnly = true)
	public List<UserDto> list(Role role, UserStatus status, String q) {
		return userRepository.search(role, status, ProductService.likePattern(q)).stream().map(UserDto::from).toList();
	}

	@Transactional
	public UserDto create(UserForm form) {
		if (form.password() == null || form.password().length() < 6) {
			throw ApiException.badRequest("Password must be at least 6 characters.");
		}
		String email = AuthService.normalizeEmail(form.email());
		if (userRepository.existsByEmail(email)) {
			throw ApiException.conflict("An account with this email already exists.");
		}
		User u = new User();
		u.setEmail(email);
		u.setPasswordHash(passwordEncoder.encode(form.password()));
		apply(u, form);
		return UserDto.from(userRepository.save(u));
	}

	@Transactional
	public UserDto update(AuthUser me, Long id, UserForm form) {
		User u = find(id);
		String email = AuthService.normalizeEmail(form.email());
		if (!email.equals(u.getEmail()) && userRepository.existsByEmail(email)) {
			throw ApiException.conflict("Another account already uses this email.");
		}
		if (u.getId().equals(me.id()) && (form.role() != Role.ADMIN || form.status() == UserStatus.INACTIVE)) {
			throw ApiException.badRequest("You cannot remove your own admin role or deactivate yourself.");
		}
		u.setEmail(email);
		if (form.password() != null && !form.password().isBlank()) {
			if (form.password().length() < 6) {
				throw ApiException.badRequest("Password must be at least 6 characters.");
			}
			u.setPasswordHash(passwordEncoder.encode(form.password()));
		}
		apply(u, form);
		return UserDto.from(u);
	}

	@Transactional
	public UserDto setStatus(AuthUser me, Long id, UserStatus status) {
		if (id.equals(me.id())) {
			throw ApiException.badRequest("You cannot change your own status.");
		}
		User u = find(id);
		u.setStatus(status);
		return UserDto.from(u);
	}

	/** Users with orders are kept for history; deactivate them instead. */
	@Transactional
	public void delete(AuthUser me, Long id) {
		if (id.equals(me.id())) {
			throw ApiException.badRequest("You cannot delete your own account.");
		}
		User u = find(id);
		if (orderRepository.existsByBuyerIdOrSupplierId(id, id)) {
			throw ApiException.conflict(u.getFullName() + " has orders and cannot be deleted. Make the user Inactive instead.");
		}
		userRepository.delete(u);
	}

	private User find(Long id) {
		return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User"));
	}

	private static void apply(User u, UserForm f) {
		u.setFullName(f.fullName().trim());
		u.setRole(f.role());
		u.setStatus(f.status() == null ? UserStatus.ACTIVE : f.status());
		u.setPhone(f.phone());
		u.setCompanyName(f.companyName());
		u.setAddress(f.address());
		u.setCity(f.city());
		u.setCountry(f.country());
		u.setAbout(f.about());
	}

}
