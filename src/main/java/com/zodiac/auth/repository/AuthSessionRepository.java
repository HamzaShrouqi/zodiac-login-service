package com.zodiac.auth.repository;

import com.zodiac.auth.entity.AuthSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

	/** Find a session by its token hash, loading its user in the same query ("join fetch"). */
	@Query("SELECT s FROM AuthSession s JOIN FETCH s.user WHERE s.tokenHash = :tokenHash")
	Optional<AuthSession> findByTokenHashWithUser(String tokenHash);

	/** Log out every other device of this user (used after a password change). Returns how many were revoked. */
	@Modifying
	@Query("""
			UPDATE AuthSession s SET s.revokedAt = :now
			WHERE s.user.id = :userId AND s.id <> :keepSessionId AND s.revokedAt IS NULL
			""")
	int revokeAllOtherSessions(UUID userId, UUID keepSessionId, Instant now);

}
