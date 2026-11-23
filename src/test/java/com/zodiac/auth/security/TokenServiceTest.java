package com.zodiac.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit test: no Spring, no database. Just the class, called directly. */
class TokenServiceTest {

	private final TokenService tokenService = new TokenService();

	@Test
	void generatesTokensThatAreLongAndUrlSafe() {
		String token = tokenService.generateToken();

		assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
	}

	@Test
	void neverGeneratesTheSameTokenTwice() {
		Set<String> tokens = new HashSet<>();
		for (int i = 0; i < 1_000; i++) {
			tokens.add(tokenService.generateToken());
		}

		assertThat(tokens).hasSize(1_000);
	}

	@Test
	void hashIsStable64CharacterHex() {
		String hash = tokenService.hash("some-token");

		assertThat(hash).hasSize(64).matches("[0-9a-f]+");
		assertThat(tokenService.hash("some-token")).isEqualTo(hash);
	}

	@Test
	void hashMatchesKnownSha256Value() {
		// SHA-256 of "abc" is a famous published test value, so this proves we hash correctly.
		assertThat(tokenService.hash("abc"))
				.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	@Test
	void differentTokensGiveDifferentHashes() {
		assertThat(tokenService.hash("token-a")).isNotEqualTo(tokenService.hash("token-b"));
	}

}
