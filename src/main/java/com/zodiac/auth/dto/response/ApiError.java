package com.zodiac.auth.dto.response;

import java.time.Instant;
import java.util.Map;

/**
 * The single JSON shape for EVERY error this service returns, e.g.
 * <pre>
 * { "code": "EMAIL_ALREADY_REGISTERED",
 *   "message": "An account with this email already exists.",
 *   "fieldErrors": {},
 *   "timestamp": "2026-11-23T10:00:00Z" }
 * </pre>
 * "code" is for programs (the frontend can translate it, e.g. to Arabic later).
 * "message" is for humans. "fieldErrors" says which input field was wrong, if any.
 */
public record ApiError(
		String code,
		String message,
		Map<String, String> fieldErrors,
		Instant timestamp) {

	public static ApiError of(String code, String message) {
		return new ApiError(code, message, Map.of(), Instant.now());
	}

	public static ApiError withFieldErrors(String code, String message, Map<String, String> fieldErrors) {
		return new ApiError(code, message, fieldErrors, Instant.now());
	}

}
