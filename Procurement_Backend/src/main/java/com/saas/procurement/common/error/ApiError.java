package com.saas.procurement.common.error;

import java.time.Instant;
import java.util.List;

import org.slf4j.MDC;

import com.saas.procurement.common.web.TraceIdFilter;

/**
 * Public error body (blueprint 6.2). Never contains stack traces, SQL or internal details.
 */
public record ApiError(
		Instant timestamp,
		int status,
		String code,
		String message,
		List<FieldError> fieldErrors,
		String traceId) {

	public record FieldError(String field, String reason) {
	}

	public static ApiError of(ErrorCode code) {
		return of(code, code.defaultMessage(), List.of());
	}

	public static ApiError of(ErrorCode code, String message) {
		return of(code, message, List.of());
	}

	public static ApiError of(ErrorCode code, String message, List<FieldError> fieldErrors) {
		return new ApiError(Instant.now(), code.status().value(), code.name(), message, fieldErrors,
				MDC.get(TraceIdFilter.MDC_KEY));
	}

}
