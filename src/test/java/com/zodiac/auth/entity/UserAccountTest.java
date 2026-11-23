package com.zodiac.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** The lockout rule lives inside UserAccount, so we test it directly, with no database. */
class UserAccountTest {

	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final Duration LOCK = Duration.ofMinutes(15);

	private final UserAccount user = new UserAccount("sara@zodiac.test", "hash", Role.CUSTOMER);

	@Test
	void newAccountIsActiveAndNotLocked() {
		assertThat(user.isActive()).isTrue();
		assertThat(user.isLockedAt(NOW)).isFalse();
		assertThat(user.getFailedLoginAttempts()).isZero();
	}

	@Test
	void failedLoginsBelowTheLimitDoNotLock() {
		for (int i = 0; i < 4; i++) {
			user.recordFailedLogin(NOW, 5, LOCK);
		}

		assertThat(user.getFailedLoginAttempts()).isEqualTo(4);
		assertThat(user.isLockedAt(NOW)).isFalse();
	}

	@Test
	void reachingTheLimitLocksForTheConfiguredTimeAndResetsTheCounter() {
		for (int i = 0; i < 5; i++) {
			user.recordFailedLogin(NOW, 5, LOCK);
		}

		assertThat(user.isLockedAt(NOW)).isTrue();
		assertThat(user.getLockedUntil()).isEqualTo(NOW.plus(LOCK));
		assertThat(user.getFailedLoginAttempts()).isZero();
	}

	@Test
	void lockEndsByItselfWhenTimeIsUp() {
		for (int i = 0; i < 5; i++) {
			user.recordFailedLogin(NOW, 5, LOCK);
		}

		assertThat(user.isLockedAt(NOW.plus(LOCK).minusSeconds(1))).isTrue();
		assertThat(user.isLockedAt(NOW.plus(LOCK))).isFalse();
	}

	@Test
	void successfulLoginClearsCounterAndRecordsTime() {
		user.recordFailedLogin(NOW, 5, LOCK);
		user.recordFailedLogin(NOW, 5, LOCK);

		user.recordSuccessfulLogin(NOW);

		assertThat(user.getFailedLoginAttempts()).isZero();
		assertThat(user.getLastLoginAt()).isEqualTo(NOW);
	}

}
