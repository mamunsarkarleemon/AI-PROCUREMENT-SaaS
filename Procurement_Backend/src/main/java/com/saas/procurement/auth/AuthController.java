package com.saas.procurement.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.auth.AuthDtos.LoginRequest;
import com.saas.procurement.auth.AuthDtos.LoginResponse;
import com.saas.procurement.auth.AuthDtos.PasswordChange;
import com.saas.procurement.auth.AuthDtos.ProfileUpdate;
import com.saas.procurement.auth.AuthDtos.RegisterRequest;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.dto.UserDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/** Public: create a Buyer or Supplier account. */
	@PostMapping("/auth/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserDto register(@Valid @RequestBody RegisterRequest req) {
		return authService.register(req);
	}

	/** Public: returns a token to send as "Authorization: Bearer <token>". */
	@PostMapping("/auth/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest req) {
		return authService.login(req);
	}

	@GetMapping("/me")
	public UserDto me(@AuthenticationPrincipal AuthUser me) {
		return authService.me(me.id());
	}

	@PutMapping("/me")
	public UserDto updateProfile(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProfileUpdate req) {
		return authService.updateProfile(me.id(), req);
	}

	@PutMapping("/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PasswordChange req) {
		authService.changePassword(me.id(), req);
	}

}
