package com.zodiac.auth.service;

import com.zodiac.auth.config.AuthProperties;
import com.zodiac.auth.dto.request.ChangePasswordRequest;
import com.zodiac.auth.dto.request.LoginRequest;
import com.zodiac.auth.dto.request.RegisterRequest;
import com.zodiac.auth.dto.response.LoginResponse;
import com.zodiac.auth.dto.response.UserResponse;
import com.zodiac.auth.entity.Role;
import com.zodiac.auth.entity.UserAccount;
import com.zodiac.auth.exception.ApiException;
import com.zodiac.auth.exception.ErrorCode;
import com.zodiac.auth.repository.UserAccountRepository;
import com.zodiac.auth.security.CurrentSession;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The login desk's rules: register, log in, who-am-I, change password. */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	/** BCrypt only looks at the first 72 bytes of a password; longer ones are rejected, not silently cut. */
	private static final int BCRYPT_MAX_BYTES = 72;

	private final UserAccountRepository userRepository;
	private final SessionService sessionService;
	private final PasswordEncoder passwordEncoder;
	private final AuthProperties properties;
	private final Clock clock;

	/**
	 * A hash of a throwaway password. When someone logs in with an email that doesn't exist, we
	 * still check a password against this, so the answer takes the same time either way.
	 * Otherwise an attacker could time responses to discover which emails have accounts.
	 */
	private final String dummyPasswordHash;

	public AuthService(UserAccountRepository userRepository, SessionService sessionService,
			PasswordEncoder passwordEncoder, AuthProperties properties, Clock clock) {
		this.userRepository = userRepository;
		this.sessionService = sessionService;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
		this.clock = clock;
		this.dummyPasswordHash = passwordEncoder.encode("timing-protection-dummy-password");
	}

	/** Creates a new CUSTOMER account. Staff and admin accounts are not created through this. */
	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}

		UserAccount user = userRepository.save(new UserAccount(email, hashPassword(request.password()), Role.CUSTOMER));
		log.info("Registered new account id={}", user.getId()); // the id, never the email or password
		return UserResponse.from(user);
	}

	/**
	 * Checks email + password and returns a new session token.
	 *
	 * <p>noRollbackFor: normally a thrown exception UNDOES the transaction's changes. But on a wrong
	 * password we throw AND need to keep the increased failed-attempt counter, so we tell Spring
	 * not to undo on our ApiException.
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
		Instant now = clock.instant();
		UserAccount user = userRepository.findByEmail(normalizeEmail(request.email())).orElse(null);

		if (user == null) {
			passwordEncoder.matches(request.password(), dummyPasswordHash); // same timing, see field comment
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
		}
		if (user.isLockedAt(now)) {
			throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			user.recordFailedLogin(now, properties.maxFailedLoginAttempts(), properties.lockDuration());
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
		}
		if (!user.isActive()) {
			throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
		}

		user.recordSuccessfulLogin(now);
		SessionService.NewSession session = sessionService.createSession(user, ipAddress, userAgent);
		log.info("Login succeeded for account id={}", user.getId());
		return LoginResponse.bearer(session.token(), session.expiresAt(), UserResponse.from(user));
	}

	@Transactional(readOnly = true)
	public UserResponse getUser(UUID userId) {
		return userRepository.findById(userId)
				.map(UserResponse::from)
				.orElseThrow(() -> new ApiException(ErrorCode.INVALID_SESSION));
	}

	/** Changes the password, then logs out every OTHER device (this one stays logged in). */
	@Transactional
	public void changePassword(CurrentSession current, ChangePasswordRequest request) {
		UserAccount user = userRepository.findById(current.userId())
				.orElseThrow(() -> new ApiException(ErrorCode.INVALID_SESSION));

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(ErrorCode.INCORRECT_CURRENT_PASSWORD);
		}

		user.changePasswordHash(hashPassword(request.newPassword()));
		int revoked = sessionService.revokeAllOtherSessions(user.getId(), current.sessionId());
		log.info("Password changed for account id={}, other sessions revoked={}", user.getId(), revoked);
	}

	// ---- helpers ----

	/** "  Sara@Zodiac.COM " → "sara@zodiac.com", so the same person can't register twice. */
	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String hashPassword(String rawPassword) {
		// @Size counts characters, but BCrypt's limit is in bytes (e.g. an Arabic letter takes 2 bytes).
		if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
			throw new ApiException(ErrorCode.PASSWORD_TOO_LONG);
		}
		return passwordEncoder.encode(rawPassword);
	}

}
