package com.zodiac.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** JSON body of PUT /api/v1/auth/password. */
public record ChangePasswordRequest(
		@Schema(example = "rose-oud-2026")
		@NotBlank String currentPassword,
		@Schema(example = "musk-amber-2026", description = "8 to 72 characters")
		@NotBlank @Size(min = 8, max = 72) String newPassword) {
}
