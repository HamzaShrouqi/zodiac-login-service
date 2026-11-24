package com.zodiac.auth.controller;

import com.zodiac.auth.config.OpenApiConfig;
import com.zodiac.auth.dto.request.ChangePasswordRequest;
import com.zodiac.auth.dto.request.LoginRequest;
import com.zodiac.auth.dto.request.RegisterRequest;
import com.zodiac.auth.dto.response.ApiError;
import com.zodiac.auth.dto.response.LoginResponse;
import com.zodiac.auth.dto.response.UserResponse;
import com.zodiac.auth.security.BearerToken;
import com.zodiac.auth.security.CurrentSession;
import com.zodiac.auth.service.AuthService;
import com.zodiac.auth.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP "front counter". It only translates HTTP ↔ Java: read the request, call a service,
 * return the result. No business rules live here; they are in the services.
 *
 * <p>The {@code @Operation} / {@code @ApiResponse} annotations change nothing at runtime. They are
 * read by springdoc to build the documentation page.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, log in and manage your session")
public class AuthController {

	private final AuthService authService;
	private final SessionService sessionService;

	public AuthController(AuthService authService, SessionService sessionService) {
		this.authService = authService;
		this.sessionService = sessionService;
	}

	@Operation(summary = "Create a customer account",
			description = "Emails are trimmed and stored in lowercase. Staff and admin accounts are not created here.")
	@ApiResponse(responseCode = "201", description = "Account created")
	@ApiResponse(responseCode = "400", description = "VALIDATION_FAILED, MALFORMED_REQUEST or PASSWORD_TOO_LONG", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_REGISTERED", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@Operation(summary = "Log in",
			description = "Returns an access token. Send it on later requests as `Authorization: Bearer <accessToken>`.")
	@ApiResponse(responseCode = "200", description = "Logged in")
	@ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS (same answer for a wrong password and an unknown email)", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@ApiResponse(responseCode = "423", description = "ACCOUNT_LOCKED: too many wrong passwords, try again later", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		return authService.login(request, httpRequest.getRemoteAddr(), httpRequest.getHeader(HttpHeaders.USER_AGENT));
	}

	@Operation(summary = "Log out", description = "Ends this session. The token stops working immediately.",
			security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
	@ApiResponse(responseCode = "204", description = "Logged out")
	@ApiResponse(responseCode = "401", description = "INVALID_SESSION", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		CurrentSession current = requireSession(authorization);
		sessionService.revoke(current.sessionId());
	}

	@Operation(summary = "Who am I?", description = "Returns the account that owns the token.",
			security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
	@ApiResponse(responseCode = "200", description = "The logged-in user")
	@ApiResponse(responseCode = "401", description = "INVALID_SESSION: missing, invalid, expired or logged-out token", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@GetMapping("/me")
	public UserResponse me(@Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		CurrentSession current = requireSession(authorization);
		return authService.getUser(current.userId());
	}

	@Operation(summary = "Change password",
			description = "This session stays logged in; the account's other sessions are ended.",
			security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
	@ApiResponse(responseCode = "204", description = "Password changed")
	@ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or INCORRECT_CURRENT_PASSWORD", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@ApiResponse(responseCode = "401", description = "INVALID_SESSION", content = @Content(schema = @Schema(implementation = ApiError.class)))
	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(
			@Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody ChangePasswordRequest request) {
		CurrentSession current = requireSession(authorization);
		authService.changePassword(current, request);
	}

	/** Every protected endpoint starts here: no valid token → 401 INVALID_SESSION. */
	private CurrentSession requireSession(String authorizationHeader) {
		return sessionService.authenticate(BearerToken.extract(authorizationHeader));
	}

}
