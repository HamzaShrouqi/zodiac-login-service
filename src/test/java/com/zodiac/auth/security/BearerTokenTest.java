package com.zodiac.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zodiac.auth.exception.ApiException;
import com.zodiac.auth.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BearerTokenTest {

	@Test
	void extractsTokenFromValidHeader() {
		assertThat(BearerToken.extract("Bearer abc123")).isEqualTo("abc123");
	}

	/** One test method, run once for each bad header below. */
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "abc123", "Basic abc123", "bearer abc123", "Bearer", "Bearer ", "Bearer    " })
	void rejectsMissingOrMalformedHeader(String header) {
		assertThatThrownBy(() -> BearerToken.extract(header))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_SESSION));
	}

}
