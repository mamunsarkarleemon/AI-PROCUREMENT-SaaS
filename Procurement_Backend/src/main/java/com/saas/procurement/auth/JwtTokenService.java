package com.saas.procurement.auth;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.saas.procurement.company.CompanyMember;
import com.saas.procurement.config.JwtConfig;
import com.saas.procurement.config.JwtProperties;
import com.saas.procurement.user.User;

/**
 * Issues short-lived access tokens. Claims are kept minimal: subject (user id), email and roles.
 * Services must still check company membership from the database for company-owned data.
 */
@Service
public class JwtTokenService {

	private final JwtEncoder encoder;
	private final JwtProperties properties;

	public JwtTokenService(JwtEncoder encoder, JwtProperties properties) {
		this.encoder = encoder;
		this.properties = properties;
	}

	public IssuedToken issue(User user, List<CompanyMember> memberships) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.accessTokenTtl());

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.id(UUID.randomUUID().toString())
				.issuer(properties.issuer())
				.audience(List.of(properties.audience()))
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim("email", user.getEmail())
				.claim(JwtConfig.ROLES_CLAIM, rolesOf(user, memberships))
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(token, properties.accessTokenTtl().toSeconds());
	}

	/** e.g. ["USER", "BUYER", "BUYER_OWNER"] -> authorities ROLE_USER, ROLE_BUYER, ROLE_BUYER_OWNER. */
	static List<String> rolesOf(User user, List<CompanyMember> memberships) {
		Set<String> roles = new LinkedHashSet<>();
		roles.add(user.getPlatformRole().name());
		for (CompanyMember m : memberships) {
			roles.add(m.getRole().companyType().name());
			roles.add(m.getRole().name());
		}
		return List.copyOf(roles);
	}

	public record IssuedToken(String value, long expiresInSeconds) {
	}

}
