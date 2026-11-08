package com.saas.procurement.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.saas.procurement.security.DbUserAuthenticationConverter;
import com.saas.procurement.security.SecurityErrorHandlers;

/**
 * Public: landing page data, register, login, uploaded files.
 * Everything else needs "Authorization: Bearer <token>".
 * Role / menu checks are on the controllers with @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
			"/api/v1/auth/**",
			"/api/v1/public/**",
			"/uploads/**",
			"/actuator/health",
			"/v3/api-docs/**",
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/error"
	};

	private final SecurityErrorHandlers errorHandlers;
	private final DbUserAuthenticationConverter authenticationConverter;
	private final List<String> allowedOrigins;

	public SecurityConfig(SecurityErrorHandlers errorHandlers, DbUserAuthenticationConverter authenticationConverter,
			@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		this.errorHandlers = errorHandlers;
		this.authenticationConverter = authenticationConverter;
		this.allowedOrigins = allowedOrigins;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable) // token in header, no cookies
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
						.authenticationEntryPoint(errorHandlers)
						.accessDeniedHandler(errorHandlers))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(errorHandlers)
						.accessDeniedHandler(errorHandlers));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

}
