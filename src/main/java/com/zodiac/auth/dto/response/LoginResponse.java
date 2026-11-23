package com.zodiac.auth.dto.response;

import java.time.Instant;

/**
 * Returned after a successful login. The client keeps accessToken and sends it back on later
 * requests as the header:  Authorization: Bearer &lt;accessToken&gt;
 * This is the only time the raw token is ever shown; the server keeps just its hash.
 */
public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		UserResponse user) {

	public static LoginResponse bearer(String accessToken, Instant expiresAt, UserResponse user) {
		return new LoginResponse(accessToken, "Bearer", expiresAt, user);
	}

}
