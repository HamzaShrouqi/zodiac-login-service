package com.zodiac.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/v1/auth/register.
 * The annotations are checked automatically (@Valid in the controller) before our code runs.
 * Max 72 characters because BCrypt ignores everything after 72 bytes.
 */
public record RegisterRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8, max = 72) String password) {

	/**
	 * Runs as soon as the JSON becomes this object, BEFORE validation.
	 * Trims spaces people accidentally paste around their email ("  sara@x.com ").
	 * Passwords are left exactly as typed: spaces can be part of a password.
	 */
	public RegisterRequest {
		email = email == null ? null : email.trim();
	}

}
