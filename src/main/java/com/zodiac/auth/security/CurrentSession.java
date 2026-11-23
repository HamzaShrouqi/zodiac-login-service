package com.zodiac.auth.security;

import java.util.UUID;

/** "Who is making this request?" The result of checking a valid token. */
public record CurrentSession(UUID sessionId, UUID userId) {
}
