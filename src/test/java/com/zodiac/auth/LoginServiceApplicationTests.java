package com.zodiac.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Smoke test: the whole application, including the database and migrations, can start. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LoginServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
