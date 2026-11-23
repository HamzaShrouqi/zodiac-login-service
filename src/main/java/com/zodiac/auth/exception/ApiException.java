package com.zodiac.auth.exception;

/**
 * Thrown by our code when a request breaks a business rule, e.g.
 * {@code throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);}
 * GlobalExceptionHandler turns it into the right HTTP status + JSON body.
 */
public class ApiException extends RuntimeException {

	private final ErrorCode errorCode;

	public ApiException(ErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

}
