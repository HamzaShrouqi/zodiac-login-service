package com.zodiac.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zodiac.auth.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Checks that the Liquibase migrations build the schema we designed, on an empty database.
 * If someone breaks a migration, this test fails before the change can reach any real environment.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseMigrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void createsBothTables() {
		List<String> tables = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
				String.class);

		assertThat(tables).contains("user_account", "auth_session");
	}

	@Test
	void recordsEveryChangeSetAsApplied() {
		List<String> applied = jdbc.queryForList(
				"SELECT id FROM databasechangelog ORDER BY orderexecuted", String.class);

		assertThat(applied).containsExactly(
				"001-create-user-account",
				"001-user-account-check-constraints",
				"002-create-auth-session",
				"002-auth-session-user-id-index");
	}

	@Test
	void rejectsUnknownRole() {
		assertThatThrownBy(() -> insertUser("seller@zodiac.test", "SELLER"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_user_account_role");
	}

	@Test
	void rejectsEmailThatIsNotLowercase() {
		assertThatThrownBy(() -> insertUser("Sara@Zodiac.test", "CUSTOMER"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_user_account_email_lowercase");
	}

	private void insertUser(String email, String role) {
		jdbc.update("""
				INSERT INTO user_account (id, email, password_hash, role, created_at, updated_at)
				VALUES (gen_random_uuid(), ?, 'not-a-real-hash', ?, now(), now())
				""", email, role);
	}

}
