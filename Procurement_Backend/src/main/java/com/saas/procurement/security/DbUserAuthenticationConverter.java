package com.saas.procurement.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.menu.MenuService;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;

/**
 * Runs on every request with a valid token: loads the user from the database and gives them
 * - ROLE_ADMIN / ROLE_BUYER / ROLE_SUPPLIER
 * - MENU_xxx for every menu the admin has enabled for their role
 * Inactive or deleted users are rejected (401) even if their token has not expired yet.
 */
@Component
public class DbUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final UserRepository userRepository;
	private final MenuService menuService;

	public DbUserAuthenticationConverter(UserRepository userRepository, MenuService menuService) {
		this.userRepository = userRepository;
		this.menuService = menuService;
	}

	@Override
	@Transactional(readOnly = true)
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Long userId = Long.valueOf(jwt.getSubject());
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new DisabledException("User no longer exists"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new DisabledException("User is inactive");
		}

		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
		menuService.enabledMenuKeys(user.getRole())
				.forEach(key -> authorities.add(new SimpleGrantedAuthority("MENU_" + key)));

		AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
		return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
	}

}
