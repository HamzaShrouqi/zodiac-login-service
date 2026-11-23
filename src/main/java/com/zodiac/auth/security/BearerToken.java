package com.zodiac.auth.security;

import com.zodiac.auth.exception.ApiException;
import com.zodiac.auth.exception.ErrorCode;

/** Reads the token out of an "Authorization: Bearer &lt;token&gt;" HTTP header. */
public final class BearerToken {

	private static final String PREFIX = "Bearer ";

	private BearerToken() {
	}

	public static String extract(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
			throw new ApiException(ErrorCode.INVALID_SESSION);
		}
		String token = authorizationHeader.substring(PREFIX.length()).trim();
		if (token.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_SESSION);
		}
		return token;
	}

}
