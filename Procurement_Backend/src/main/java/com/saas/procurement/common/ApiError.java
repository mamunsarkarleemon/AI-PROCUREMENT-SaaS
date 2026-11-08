package com.saas.procurement.common;

import java.time.Instant;
import java.util.Map;

/** The JSON body of every error: {"status":400,"message":"...","fieldErrors":{"email":"..."}} */
public record ApiError(Instant timestamp, int status, String message, Map<String, String> fieldErrors) {

	public static ApiError of(int status, String message) {
		return new ApiError(Instant.now(), status, message, Map.of());
	}

}
