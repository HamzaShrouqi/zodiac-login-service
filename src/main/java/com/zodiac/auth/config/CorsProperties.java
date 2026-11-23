package com.zodiac.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which websites are allowed to call this API from a browser, read from {@code zodiac.cors.*}.
 * The default is an EMPTY list: no other website may call us until we explicitly say so.
 * Locally: application-local.yml. In production: the ZODIAC_CORS_ALLOWED_ORIGINS environment variable.
 */
@ConfigurationProperties(prefix = "zodiac.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
	}

}
