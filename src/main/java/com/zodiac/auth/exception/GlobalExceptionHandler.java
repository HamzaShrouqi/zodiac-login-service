package com.zodiac.auth.exception;

import com.zodiac.auth.dto.response.ApiError;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Catches exceptions thrown anywhere in a request and turns them into a clean ApiError JSON.
 * Without this, clients would get Spring's default error page, and possibly internal details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** Our own business errors (wrong password, email taken...). */
	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApiException(ApiException ex) {
		ErrorCode code = ex.getErrorCode();
		return ResponseEntity.status(code.status()).body(ApiError.of(code.name(), code.message()));
	}

	/** A @Valid check failed (e.g. password shorter than 8). Reports every bad field. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest()
				.body(ApiError.withFieldErrors("VALIDATION_FAILED", "Some fields are invalid.", fieldErrors));
	}

	/** The body isn't valid JSON at all. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest().body(ApiError.of("MALFORMED_REQUEST", "Request body is missing or not valid JSON."));
	}

	/**
	 * A database rule blocked the write. Normally our code checks first (e.g. "email taken"),
	 * but if two requests race, the unique constraint in the database is the final safety net.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException ex) {
		log.warn("Database constraint violation: {}", ex.getMostSpecificCause().getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of("CONFLICT", "The request conflicts with existing data."));
	}

	/**
	 * Everything else. Spring's own errors (unknown URL → 404, wrong HTTP method → 405...) keep their
	 * status. Real bugs become a 500 with a generic message: details go to the log, never to the client.
	 */
	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleEverythingElse(Exception ex) {
		if (ex instanceof ErrorResponse springError) {
			HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
			return ResponseEntity.status(status).body(ApiError.of(status.name(), status.getReasonPhrase()));
		}
		log.error("Unexpected error", ex);
		return ResponseEntity.internalServerError()
				.body(ApiError.of("INTERNAL_ERROR", "Something went wrong on our side."));
	}

}
