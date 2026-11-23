package com.zodiac.auth.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Shared building blocks ("beans") that other classes ask for in their constructors. */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AppConfig {

	/** BCrypt: a deliberately slow, salted password hash. The same password gives a different hash every time. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * The current time comes from a Clock bean instead of calling Instant.now() everywhere.
	 * In tests we can swap in a fixed clock and "fast-forward" time (e.g. to test an expired session).
	 */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
