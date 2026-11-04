package com.saas.procurement.common.error;

import org.springframework.http.HttpStatus;

/**
 * Stable, public error codes. The React app and API clients switch on {@code code},
 * never on the human-readable message.
 */
public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "One or more fields are invalid."),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request body is missing or malformed."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "The access token is invalid or has expired."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password."),
	ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "This account is not active."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint."),
	EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "An account with this email already exists."),
	COMPANY_ALREADY_REGISTERED(HttpStatus.CONFLICT,
			"A company with this registration number is already registered in this country."),
	CONFLICT(HttpStatus.CONFLICT, "The request conflicts with the current state of the resource."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String defaultMessage) {
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	public HttpStatus status() {
		return status;
	}

	public String defaultMessage() {
		return defaultMessage;
	}

}
