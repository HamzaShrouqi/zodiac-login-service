package com.zodiac.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zodiac.auth.config.AuthProperties;
import com.zodiac.auth.dto.request.ChangePasswordRequest;
import com.zodiac.auth.dto.request.LoginRequest;
import com.zodiac.auth.dto.request.RegisterRequest;
import com.zodiac.auth.dto.response.LoginResponse;
import com.zodiac.auth.dto.response.UserResponse;
import com.zodiac.auth.entity.AccountStatus;
import com.zodiac.auth.entity.Role;
import com.zodiac.auth.entity.UserAccount;
import com.zodiac.auth.exception.ApiException;
import com.zodiac.auth.exception.ErrorCode;
import com.zodiac.auth.repository.UserAccountRepository;
import com.zodiac.auth.security.CurrentSession;
import com.zodiac.auth.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests the business rules in AuthService WITHOUT a database.
 * The repository and session service are "mocks": stand-in actors we can script
 * ("when asked for this email, answer with that user") and then question ("were you called?").
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
	private static final String PASSWORD = "rose-oud-2026";

	@Mock
	private UserAccountRepository userRepository;
	@Mock
	private SessionService sessionService;

	// A real BCrypt, but with the cheapest setting (4) so the tests run fast.
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
	private final AuthProperties properties = new AuthProperties(Duration.ofDays(7), 5, Duration.ofMinutes(15));
	private final MutableClock clock = new MutableClock(START);

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, sessionService, passwordEncoder, properties, clock);
	}

	// ---------- register ----------

	@Test
	void registerCreatesCustomerWithHashedPasswordAndLowercaseEmail() {
		when(userRepository.existsByEmail("sara@zodiac.test")).thenReturn(false);
		when(userRepository.save(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));

		UserResponse response = authService.register(new RegisterRequest("Sara@Zodiac.TEST", PASSWORD));

		ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("sara@zodiac.test");
		assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
		assertThat(saved.getValue().getPasswordHash()).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, saved.getValue().getPasswordHash())).isTrue();
		assertThat(response.email()).isEqualTo("sara@zodiac.test");
	}

	@Test
	void registerRejectsEmailThatIsAlreadyTaken() {
		when(userRepository.existsByEmail("sara@zodiac.test")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest("SARA@zodiac.test", PASSWORD)))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED));
		verify(userRepository, never()).save(any());
	}

	@Test
	void registerRejectsPasswordOverBcryptLimitMeasuredInBytes() {
		// 40 Arabic letters = 40 characters (passes @Size) but 80 bytes (BCrypt would silently cut it).
		String longInBytes = "ع".repeat(40);

		assertThatThrownBy(() -> authService.register(new RegisterRequest("sara@zodiac.test", longInBytes)))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_TOO_LONG));
	}

	// ---------- login ----------

	@Test
	void loginSucceedsWithCorrectPassword() {
		UserAccount user = existingUser();
		when(sessionService.createSession(any(), any(), any()))
				.thenReturn(new SessionService.NewSession("raw-token", START.plus(Duration.ofDays(7))));

		LoginResponse response = authService.login(new LoginRequest(" SARA@zodiac.test ", PASSWORD), "1.2.3.4", "agent");

		assertThat(response.accessToken()).isEqualTo("raw-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(user.getLastLoginAt()).isEqualTo(START);
		verify(sessionService).createSession(user, "1.2.3.4", "agent");
	}

	@Test
	void loginWithUnknownEmailGivesTheSameErrorAsWrongPassword() {
		when(userRepository.findByEmail("nobody@zodiac.test")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@zodiac.test", PASSWORD), "ip", "ua"))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
		verify(sessionService, never()).createSession(any(), any(), any());
	}

	@Test
	void wrongPasswordCountsAsFailedAttemptAndCreatesNoSession() {
		UserAccount user = existingUser();

		assertThatThrownBy(() -> authService.login(new LoginRequest("sara@zodiac.test", "wrong-password"), "ip", "ua"))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

		assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
		verify(sessionService, never()).createSession(any(), any(), any());
	}

	@Test
	void fifthWrongPasswordLocksAccountEvenForTheCorrectPassword() {
		UserAccount user = existingUser();
		for (int i = 0; i < 5; i++) {
			assertThatThrownBy(() -> authService.login(new LoginRequest("sara@zodiac.test", "wrong"), "ip", "ua"))
					.isInstanceOf(ApiException.class);
		}

		assertThat(user.isLockedAt(clock.instant())).isTrue();
		assertThatThrownBy(() -> authService.login(new LoginRequest("sara@zodiac.test", PASSWORD), "ip", "ua"))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED));
	}

	@Test
	void lockedAccountCanLogInAgainAfterTheLockTimeIsOver() {
		existingUser();
		for (int i = 0; i < 5; i++) {
			assertThatThrownBy(() -> authService.login(new LoginRequest("sara@zodiac.test", "wrong"), "ip", "ua"))
					.isInstanceOf(ApiException.class);
		}
		when(sessionService.createSession(any(), any(), any()))
				.thenReturn(new SessionService.NewSession("raw-token", START.plus(Duration.ofDays(7))));

		clock.advance(Duration.ofMinutes(16));
		LoginResponse response = authService.login(new LoginRequest("sara@zodiac.test", PASSWORD), "ip", "ua");

		assertThat(response.accessToken()).isEqualTo("raw-token");
	}

	@Test
	void successfulLoginResetsTheFailedAttemptCounter() {
		UserAccount user = existingUser();
		when(sessionService.createSession(any(), any(), any()))
				.thenReturn(new SessionService.NewSession("raw-token", START.plus(Duration.ofDays(7))));
		for (int i = 0; i < 4; i++) {
			assertThatThrownBy(() -> authService.login(new LoginRequest("sara@zodiac.test", "wrong"), "ip", "ua"))
					.isInstanceOf(ApiException.class);
		}

		authService.login(new LoginRequest("sara@zodiac.test", PASSWORD), "ip", "ua");

		assertThat(user.getFailedLoginAttempts()).isZero();
	}

	@Test
	void disabledAccountCannotLogInEvenWithTheCorrectPassword() {
		UserAccount user = existingUser();
		ReflectionTestUtils.setField(user, "status", AccountStatus.DISABLED);

		assertThatThrownBy(() -> authService.login(new LoginRequest("sara@zodiac.test", PASSWORD), "ip", "ua"))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
	}

	// ---------- change password ----------

	@Test
	void changePasswordStoresNewHashAndLogsOutOtherDevices() {
		UserAccount user = savedUser();
		UUID currentSessionId = UUID.randomUUID();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		authService.changePassword(new CurrentSession(currentSessionId, user.getId()),
				new ChangePasswordRequest(PASSWORD, "musk-amber-2026"));

		assertThat(passwordEncoder.matches("musk-amber-2026", user.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isFalse();
		verify(sessionService).revokeAllOtherSessions(user.getId(), currentSessionId);
	}

	@Test
	void changePasswordWithWrongCurrentPasswordChangesNothing() {
		UserAccount user = savedUser();
		String hashBefore = user.getPasswordHash();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.changePassword(new CurrentSession(UUID.randomUUID(), user.getId()),
				new ChangePasswordRequest("not-my-password", "musk-amber-2026")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INCORRECT_CURRENT_PASSWORD));

		assertThat(user.getPasswordHash()).isEqualTo(hashBefore);
		verify(sessionService, never()).revokeAllOtherSessions(any(), any());
	}

	// ---------- helper ----------

	/** A saved-looking account for sara@zodiac.test, and the repository set up to find her by email (for login). */
	private UserAccount existingUser() {
		UserAccount user = savedUser();
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
		return user;
	}

	/** Just the account, with no scripted repository answers. Mockito complains about stubs a test never uses. */
	private UserAccount savedUser() {
		UserAccount user = new UserAccount("sara@zodiac.test", passwordEncoder.encode(PASSWORD), Role.CUSTOMER);
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID()); // normally the database assigns this
		return user;
	}

}
