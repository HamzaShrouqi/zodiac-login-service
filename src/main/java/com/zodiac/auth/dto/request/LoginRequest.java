package com.zodiac.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** JSON body of POST /api/v1/auth/login. No format rules here: a wrong email simply fails to log in. */
public record LoginRequest(
		@NotBlank String email,
		@NotBlank String password) {

	/** Same trimming as RegisterRequest, so " sara@x.com" can log in to "sara@x.com". */
	public LoginRequest {
		email = email == null ? null : email.trim();
	}

}
