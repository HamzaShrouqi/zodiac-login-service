package com.zodiac.auth.service;

import com.zodiac.auth.config.AuthProperties;
import com.zodiac.auth.entity.AuthSession;
import com.zodiac.auth.entity.UserAccount;
import com.zodiac.auth.exception.ApiException;
import com.zodiac.auth.exception.ErrorCode;
import com.zodiac.auth.repository.AuthSessionRepository;
import com.zodiac.auth.security.CurrentSession;
import com.zodiac.auth.security.TokenService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hands out, checks and cancels login sessions ("wristbands"). */
@Service
public class SessionService {

	private static final int MAX_USER_AGENT_LENGTH = 255;

	private final AuthSessionRepository sessionRepository;
	private final TokenService tokenService;
	private final AuthProperties properties;
	private final Clock clock;

	/** Constructor injection: Spring passes in everything this class needs. */
	public SessionService(AuthSessionRepository sessionRepository, TokenService tokenService,
			AuthProperties properties, Clock clock) {
		this.sessionRepository = sessionRepository;
		this.tokenService = tokenService;
		this.properties = properties;
		this.clock = clock;
	}

	/** The raw token (give it to the client once) and when it expires. */
	public record NewSession(String token, Instant expiresAt) {
	}

	@Transactional
	public NewSession createSession(UserAccount user, String ipAddress, String userAgent) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.sessionTtl());
		String token = tokenService.generateToken();

		sessionRepository.save(new AuthSession(user, tokenService.hash(token), now, expiresAt,
				ipAddress, truncate(userAgent, MAX_USER_AGENT_LENGTH)));

		return new NewSession(token, expiresAt);
	}

	/** Checks a token. Returns who it belongs to, or throws INVALID_SESSION (401). */
	@Transactional(readOnly = true)
	public CurrentSession authenticate(String token) {
		AuthSession session = sessionRepository.findByTokenHashWithUser(tokenService.hash(token))
				.orElseThrow(() -> new ApiException(ErrorCode.INVALID_SESSION));

		if (!session.isActiveAt(clock.instant()) || !session.getUser().isActive()) {
			throw new ApiException(ErrorCode.INVALID_SESSION);
		}
		return new CurrentSession(session.getId(), session.getUser().getId());
	}

	/** Logout: this one session stops working immediately. */
	@Transactional
	public void revoke(UUID sessionId) {
		sessionRepository.findById(sessionId).ifPresent(session -> session.revoke(clock.instant()));
		// No save() needed: inside a transaction, JPA notices the change and writes it at the end.
	}

	@Transactional
	public int revokeAllOtherSessions(UUID userId, UUID keepSessionId) {
		return sessionRepository.revokeAllOtherSessions(userId, keepSessionId, clock.instant());
	}

	private static String truncate(String value, int maxLength) {
		return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

}
