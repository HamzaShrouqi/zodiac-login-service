package com.zodiac.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** JSON body of PUT /api/v1/auth/password. */
public record ChangePasswordRequest(
		@NotBlank String currentPassword,
		@NotBlank @Size(min = 8, max = 72) String newPassword) {
}
