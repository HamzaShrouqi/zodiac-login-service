package com.zodiac.auth.repository;

import com.zodiac.auth.entity.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access for user_account. We only write the interface; Spring Data writes the
 * implementation, and even builds the SQL from the method NAME ("findByEmail" → WHERE email = ?).
 * save(), findById() and friends come for free from JpaRepository.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByEmail(String email);

	boolean existsByEmail(String email);

}
