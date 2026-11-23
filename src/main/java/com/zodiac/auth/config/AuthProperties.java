package com.zodiac.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The login rules, read from the {@code zodiac.auth.*} section of application.yml.
 * Keeping them in config (not hard-coded) means each environment can tune them without code changes.
 */
@ConfigurationProperties(prefix = "zodiac.auth")
public record AuthProperties(
		Duration sessionTtl,
		int maxFailedLoginAttempts,
		Duration lockDuration) {
}
