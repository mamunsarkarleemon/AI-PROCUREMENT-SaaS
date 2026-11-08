package com.saas.procurement.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.saas.procurement.user.User;

/** Creates the login token. The token only says WHO the user is (their id). */
@Service
public class JwtService {

	private final JwtEncoder encoder;
	private final long expirationHours;

	public JwtService(JwtEncoder encoder, @Value("${app.jwt.expiration-hours:12}") long expirationHours) {
		this.encoder = encoder;
		this.expirationHours = expirationHours;
	}

	public String createToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("procureai")
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(expirationHours, ChronoUnit.HOURS))
				.claim("role", user.getRole().name())
				.claim("name", user.getFullName())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
