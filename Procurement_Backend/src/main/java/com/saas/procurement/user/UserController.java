package com.saas.procurement.user;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.user.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserProfileService profileService;

	public UserController(UserProfileService profileService) {
		this.profileService = profileService;
	}

	/** Returns the signed-in user. Requires "Authorization: Bearer &lt;token&gt;". */
	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return profileService.getProfile(UUID.fromString(jwt.getSubject()));
	}

}
