package com.zodiac.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One row of the auth_session table: one login (one "wristband"). */
@Entity
@Table(name = "auth_session")
public class AuthSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Many sessions belong to one user. LAZY = don't load the user unless we actually need it. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private UserAccount user;

	/** SHA-256 of the token. The token itself is never stored. */
	private String tokenHash;

	private Instant createdAt;

	private Instant expiresAt;

	private Instant revokedAt;

	private String ipAddress;

	private String userAgent;

	protected AuthSession() {
	}

	public AuthSession(UserAccount user, String tokenHash, Instant createdAt, Instant expiresAt,
			String ipAddress, String userAgent) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
	}

	/** Valid = not logged out and not expired yet. */
	public boolean isActiveAt(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(Instant now) {
		if (revokedAt == null) {
			revokedAt = now;
		}
	}

	public UUID getId() {
		return id;
	}

	public UserAccount getUser() {
		return user;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

}
