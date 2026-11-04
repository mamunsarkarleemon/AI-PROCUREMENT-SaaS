package com.saas.procurement.config;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.saas.procurement.common.error.ApiError;
import com.saas.procurement.common.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes 401/403 responses produced inside the Spring Security filter chain in the same JSON
 * format as GlobalExceptionHandler, instead of an empty body or an HTML page.
 */
@Component
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public SecurityErrorHandlers(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/** 401: no token, or a token that is expired / tampered / from the wrong issuer. */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ErrorCode code = authException instanceof InvalidBearerTokenException
				? ErrorCode.INVALID_TOKEN
				: ErrorCode.UNAUTHORIZED;
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		write(response, ApiError.of(code));
	}

	/** 403: valid token but the user's role is not allowed. */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		write(response, ApiError.of(ErrorCode.FORBIDDEN));
	}

	private void write(HttpServletResponse response, ApiError body) throws IOException {
		response.setStatus(body.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}

}
