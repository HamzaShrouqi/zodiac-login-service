package com.zodiac.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the user_account table, as a Java object.
 *
 * <p>Field names are camelCase and Spring maps them to snake_case columns automatically
 * (passwordHash → password_hash). Hibernate checks this mapping at startup (ddl-auto: validate).
 *
 * <p>There are no setters. Changes go through methods with business meaning
 * (recordFailedLogin, changePasswordHash...), so the rules live in one place.
 */
@Entity
@Table(name = "user_account")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String email;

	private String phone;

	private String passwordHash;

	@Enumerated(EnumType.STRING) // store "CUSTOMER", not 0, so reordering the enum can't corrupt data
	private Role role;

	@Enumerated(EnumType.STRING)
	private AccountStatus status;

	private int failedLoginAttempts;

	private Instant lockedUntil;

	private Instant lastLoginAt;

	private Instant createdAt;

	private Instant updatedAt;

	@Version
	private Long version;

	/** Required by JPA, which creates empty objects and then fills them from the database. */
	protected UserAccount() {
	}

	public UserAccount(String email, String passwordHash, Role role) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = AccountStatus.ACTIVE;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	// ---- business behaviour ----

	public boolean isLockedAt(Instant now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	public boolean isActive() {
		return status == AccountStatus.ACTIVE;
	}

	/** One more wrong password. After too many in a row, lock the account for a while and start counting again. */
	public void recordFailedLogin(Instant now, int maxAttempts, Duration lockDuration) {
		failedLoginAttempts++;
		if (failedLoginAttempts >= maxAttempts) {
			lockedUntil = now.plus(lockDuration);
			failedLoginAttempts = 0;
		}
	}

	public void recordSuccessfulLogin(Instant now) {
		failedLoginAttempts = 0;
		lockedUntil = null;
		lastLoginAt = now;
	}

	public void changePasswordHash(String newPasswordHash) {
		this.passwordHash = newPasswordHash;
	}

	// ---- getters ----

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public AccountStatus getStatus() {
		return status;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
