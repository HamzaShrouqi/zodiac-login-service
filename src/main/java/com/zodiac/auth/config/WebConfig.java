package com.zodiac.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS: lets the Zodiac web page (served from one address) call this API (another address).
 *
 * <p>Browsers block a page from calling a different "origin" (scheme + host + port) unless that
 * server says "yes, this website may". This class is where our server says yes, but only to the
 * origins listed in configuration, and only for the API paths.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final CorsProperties corsProperties;

	public WebConfig(CorsProperties corsProperties) {
		this.corsProperties = corsProperties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (corsProperties.allowedOrigins().isEmpty()) {
			return; // nobody configured → the browser keeps blocking cross-site calls (safe default)
		}
		registry.addMapping("/api/**")
				.allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "OPTIONS")
				// Our token travels in the Authorization header, not in cookies, so we do NOT
				// enable allowCredentials. That keeps this much simpler and safer.
				.allowedHeaders("Authorization", "Content-Type")
				.maxAge(3600); // the browser may remember the "yes" for an hour
	}

}
