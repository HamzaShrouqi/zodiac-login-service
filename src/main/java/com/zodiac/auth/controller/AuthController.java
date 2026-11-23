package com.zodiac.auth.controller;

import com.zodiac.auth.dto.request.ChangePasswordRequest;
import com.zodiac.auth.dto.request.LoginRequest;
import com.zodiac.auth.dto.request.RegisterRequest;
import com.zodiac.auth.dto.response.LoginResponse;
import com.zodiac.auth.dto.response.UserResponse;
import com.zodiac.auth.security.BearerToken;
import com.zodiac.auth.security.CurrentSession;
import com.zodiac.auth.service.AuthService;
import com.zodiac.auth.service.SessionService;
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
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final SessionService sessionService;

	public AuthController(AuthService authService, SessionService sessionService) {
		this.authService = authService;
		this.sessionService = sessionService;
	}

	/** Create a customer account. 201 Created + the new user. */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	/** Log in. 200 OK + a token to send back as "Authorization: Bearer ...". */
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		return authService.login(request, httpRequest.getRemoteAddr(), httpRequest.getHeader(HttpHeaders.USER_AGENT));
	}

	/** Log out this session. 204 No Content. */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		CurrentSession current = requireSession(authorization);
		sessionService.revoke(current.sessionId());
	}

	/** Who am I? 200 OK + the logged-in user. */
	@GetMapping("/me")
	public UserResponse me(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		CurrentSession current = requireSession(authorization);
		return authService.getUser(current.userId());
	}

	/** Change password. 204 No Content. Other devices get logged out. */
	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody ChangePasswordRequest request) {
		CurrentSession current = requireSession(authorization);
		authService.changePassword(current, request);
	}

	/** Every protected endpoint starts here: no valid token → 401 INVALID_SESSION. */
	private CurrentSession requireSession(String authorizationHeader) {
		return sessionService.authenticate(BearerToken.extract(authorizationHeader));
	}

}
