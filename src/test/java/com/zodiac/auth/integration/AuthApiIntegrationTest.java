package com.zodiac.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.zodiac.auth.TestcontainersConfiguration;
import com.zodiac.auth.config.AuthProperties;
import com.zodiac.auth.security.TokenService;
import com.zodiac.auth.support.MutableClock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The whole service, for real: HTTP request → controller → service → repository → PostgreSQL.
 * Nothing is mocked except time (so we can "wait" 8 days instantly).
 *
 * <p>All tests share one database, so each test registers its OWN uniquely named user
 * and never depends on what another test did.
 */
@Import({ TestcontainersConfiguration.class, AuthApiIntegrationTest.TestClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

	private static final String BASE = "/api/v1/auth";
	private static final String PASSWORD = "rose-oud-2026";

	/** Replaces the real clock with one we control. @Primary = "when two clocks exist, use this one". */
	@TestConfiguration
	static class TestClockConfig {
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(Instant.now());
		}
	}

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private MutableClock clock;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private AuthProperties properties;
	@Autowired
	private TokenService tokenService;

	@BeforeEach
	void resetClockToNow() {
		clock.set(Instant.now());
	}

	// ---------- register ----------

	@Test
	void registerCreatesCustomerAndNeverReturnsOrStoresThePassword() throws Exception {
		String email = uniqueEmail();

		register(email.toUpperCase(), PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))   // stored lowercase
				.andExpect(jsonPath("$.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		String storedHash = jdbc.queryForObject("SELECT password_hash FROM user_account WHERE email = ?",
				String.class, email);
		assertThat(storedHash).startsWith("$2").isNotEqualTo(PASSWORD); // a BCrypt hash, not plaintext
	}

	@Test
	void registerTwiceWithSameEmailIgnoringCaseGives409() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());

		register(email.toUpperCase(), "another-password-1")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	void registerTrimsSpacesAroundTheEmail() throws Exception {
		String email = uniqueEmail();

		register("  " + email + "  ", PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void registerWithBadInputListsEveryBadField() throws Exception {
		register("not-an-email", "short")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors.email").exists())
				.andExpect(jsonPath("$.fieldErrors.password").exists());
	}

	@Test
	void brokenJsonGives400WithOurErrorShape() throws Exception {
		mockMvc.perform(post(BASE + "/register").contentType(MediaType.APPLICATION_JSON).content("{oops"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	// ---------- login / me / logout ----------

	@Test
	void fullJourney_register_login_me_logout() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());

		String token = loginAndGetToken(email, PASSWORD);

		me(token).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));

		mockMvc.perform(post(BASE + "/logout").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		me(token).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_SESSION"));
	}

	@Test
	void loginAcceptsDifferentCaseAndSpacesInEmail() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());

		login(" " + email.toUpperCase() + " ", PASSWORD).andExpect(status().isOk());
	}

	@Test
	void loginResponseTellsTheClientHowToUseTheToken() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());

		login(email, PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.expiresAt").exists())
				.andExpect(jsonPath("$.user.email").value(email));
	}

	@Test
	void wrongPasswordAndUnknownEmailGiveIdenticalAnswers() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());

		String wrongPassword = login(email, "wrong-password").andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String unknownEmail = login(uniqueEmail(), "wrong-password").andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		// Same code and message, so an attacker cannot tell which emails have accounts.
		assertThat(JsonPath.<String>read(wrongPassword, "$.code")).isEqualTo("INVALID_CREDENTIALS");
		assertThat(JsonPath.<String>read(unknownEmail, "$.code")).isEqualTo("INVALID_CREDENTIALS");
		assertThat(JsonPath.<String>read(wrongPassword, "$.message"))
				.isEqualTo(JsonPath.<String>read(unknownEmail, "$.message"));
	}

	@Test
	void databaseKeepsOnlyTheTokenHashNeverTheTokenItself() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());

		String token = loginAndGetToken(email, PASSWORD);

		Integer withHash = jdbc.queryForObject("SELECT count(*) FROM auth_session WHERE token_hash = ?",
				Integer.class, tokenService.hash(token));
		Integer withRawToken = jdbc.queryForObject("SELECT count(*) FROM auth_session WHERE token_hash = ?",
				Integer.class, token);
		assertThat(withHash).isEqualTo(1);
		assertThat(withRawToken).isZero();
	}

	@Test
	void missingOrMadeUpTokenGives401() throws Exception {
		mockMvc.perform(get(BASE + "/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_SESSION"));
		me("made-up-token").andExpect(status().isUnauthorized());
		mockMvc.perform(get(BASE + "/me").header("Authorization", "Basic abc"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sessionStopsWorkingAfterItExpires() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());
		String token = loginAndGetToken(email, PASSWORD);

		clock.advance(properties.sessionTtl().minusMinutes(1));
		me(token).andExpect(status().isOk());

		clock.advance(Duration.ofMinutes(2)); // now just past the lifetime
		me(token).andExpect(status().isUnauthorized());
	}

	// ---------- lockout ----------

	@Test
	void accountLocksAfterTooManyWrongPasswordsThenRecoversByItself() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());

		for (int i = 0; i < properties.maxFailedLoginAttempts(); i++) {
			login(email, "wrong-password").andExpect(status().isUnauthorized());
		}

		// Locked: even the correct password is refused (HTTP 423).
		login(email, PASSWORD).andExpect(status().isLocked()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

		// The lock ends by itself after the configured time.
		clock.advance(properties.lockDuration().plusSeconds(1));
		login(email, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void failedAttemptsAreSavedEvenThoughTheRequestEndsInAnError() throws Exception {
		// Guards the @Transactional(noRollbackFor = ...) detail in AuthService.login.
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());

		login(email, "wrong-password").andExpect(status().isUnauthorized());
		login(email, "wrong-password").andExpect(status().isUnauthorized());

		Integer attempts = jdbc.queryForObject("SELECT failed_login_attempts FROM user_account WHERE email = ?",
				Integer.class, email);
		assertThat(attempts).isEqualTo(2);
	}

	@Test
	void disabledAccountCannotLogInAndItsExistingSessionStopsWorking() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());
		String token = loginAndGetToken(email, PASSWORD);

		jdbc.update("UPDATE user_account SET status = 'DISABLED' WHERE email = ?", email);

		login(email, PASSWORD).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
		me(token).andExpect(status().isUnauthorized());
	}

	// ---------- change password ----------

	@Test
	void changePasswordKeepsThisDeviceLogsOutOthersAndSwitchesToTheNewPassword() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());
		String thisDevice = loginAndGetToken(email, PASSWORD);
		String otherDevice = loginAndGetToken(email, PASSWORD);

		changePassword(thisDevice, PASSWORD, "musk-amber-2026").andExpect(status().isNoContent());

		me(thisDevice).andExpect(status().isOk());
		me(otherDevice).andExpect(status().isUnauthorized());
		login(email, PASSWORD).andExpect(status().isUnauthorized());
		login(email, "musk-amber-2026").andExpect(status().isOk());
	}

	@Test
	void changePasswordWithWrongCurrentPasswordIsRefusedAndNothingChanges() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());
		String token = loginAndGetToken(email, PASSWORD);

		changePassword(token, "not-my-password", "musk-amber-2026")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INCORRECT_CURRENT_PASSWORD"));

		login(email, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void changePasswordWithoutLoginGives401() throws Exception {
		mockMvc.perform(put(BASE + "/password").contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"a\",\"newPassword\":\"long-enough-1\"}"))
				.andExpect(status().isUnauthorized());
	}

	// ---------- other errors ----------

	@Test
	void unknownUrlAndWrongMethodUseOurErrorShapeToo() throws Exception {
		mockMvc.perform(get(BASE + "/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		mockMvc.perform(get(BASE + "/login"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	// ---------- helpers: one line per API call, so the tests above read like a story ----------

	private ResultActions register(String email, String password) throws Exception {
		return mockMvc.perform(post(BASE + "/register").contentType(MediaType.APPLICATION_JSON)
				.content(json("email", email, "password", password)));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON)
				.content(json("email", email, "password", password)));
	}

	private String loginAndGetToken(String email, String password) throws Exception {
		String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private ResultActions me(String token) throws Exception {
		return mockMvc.perform(get(BASE + "/me").header("Authorization", "Bearer " + token));
	}

	private ResultActions changePassword(String token, String current, String next) throws Exception {
		return mockMvc.perform(put(BASE + "/password").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("currentPassword", current, "newPassword", next)));
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@zodiac.test";
	}

	private static String json(String k1, String v1, String k2, String v2) {
		return "{\"%s\":\"%s\",\"%s\":\"%s\"}".formatted(k1, v1, k2, v2);
	}

}
