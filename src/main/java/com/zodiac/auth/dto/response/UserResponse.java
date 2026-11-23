package com.zodiac.auth.dto.response;

import com.zodiac.auth.entity.AccountStatus;
import com.zodiac.auth.entity.Role;
import com.zodiac.auth.entity.UserAccount;
import java.time.Instant;
import java.util.UUID;

/**
 * What the outside world may see about a user. Note what is NOT here: password hash,
 * failed attempts, version... The entity never leaves the service; only this copy does.
 */
public record UserResponse(
		UUID id,
		String email,
		Role role,
		AccountStatus status,
		Instant createdAt) {

	public static UserResponse from(UserAccount user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
	}

}
