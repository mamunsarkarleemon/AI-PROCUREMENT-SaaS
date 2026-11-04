package com.saas.procurement.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless JWT security for the REST API (blueprint Appendix E).
 * Authentication = "who are you" (JWT). Authorization = role checks with @PreAuthorize plus
 * company/object checks inside services (Appendix B and F).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
			"/api/v1/auth/register",
			"/api/v1/auth/login",
			"/api/v1/auth/verify-email",
			"/api/v1/auth/forgot-password",
			"/api/v1/auth/reset-password",
			"/actuator/health",
			"/actuator/info",
			"/v3/api-docs/**",
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/error"
	};

	private final SecurityErrorHandlers errorHandlers;
	private final JwtAuthenticationConverter jwtAuthenticationConverter;
	private final List<String> allowedOrigins;

	public SecurityConfig(SecurityErrorHandlers errorHandlers, JwtAuthenticationConverter jwtAuthenticationConverter,
			@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		this.errorHandlers = errorHandlers;
		this.jwtAuthenticationConverter = jwtAuthenticationConverter;
		this.allowedOrigins = allowedOrigins;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// Stateless bearer-token API: the token is sent in the Authorization header, not a cookie,
				// so CSRF protection is not needed. Revisit this if cookies are introduced.
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// No HTML login page or browser popup - this is a JSON API for the React app.
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.anyRequest().authenticated())
				// Validate "Authorization: Bearer <jwt>" on every protected request.
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(errorHandlers)
						.accessDeniedHandler(errorHandlers))
				// JSON 401/403 instead of redirects or empty bodies.
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(errorHandlers)
						.accessDeniedHandler(errorHandlers));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		// Explicit allowlist only. Never use "*" together with credentials.
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		config.setExposedHeaders(List.of("X-Trace-Id"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

}
