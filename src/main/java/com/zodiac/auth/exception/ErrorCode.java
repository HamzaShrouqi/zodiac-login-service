package com.zodiac.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Every business error this service can report: its HTTP status and its human message, in one list.
 * HTTP status codes tell the client what KIND of problem it is:
 * 400 = your input is wrong, 401 = we don't know who you are, 403 = we know you but you can't,
 * 409 = conflicts with existing data, 423 = locked.
 */
public enum ErrorCode {

	EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "An account with this email already exists."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."),
	ACCOUNT_LOCKED(HttpStatus.LOCKED, "Too many failed login attempts. Please try again later."),
	ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "This account is disabled."),
	INVALID_SESSION(HttpStatus.UNAUTHORIZED, "Missing, invalid or expired session. Please log in again."),
	INCORRECT_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "The current password is incorrect."),
	PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "Password is too long (maximum 72 bytes).");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	public String message() {
		return message;
	}

}
