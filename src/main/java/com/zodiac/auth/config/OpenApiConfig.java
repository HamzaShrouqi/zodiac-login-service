package com.zodiac.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The title and description at the top of the API documentation page, plus the definition of
 * "bearerAuth": how a client proves who it is. It adds the green "Authorize" button to Swagger UI,
 * where you paste the token from /login once and every protected endpoint then uses it.
 */
@Configuration
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	OpenAPI zodiacOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Zodiac Login Service API")
						.version("v1")
						.description("""
								Accounts and login sessions for the Zodiac boutique platform.

								**How to try it:** register an account, log in, copy the `accessToken` from the
								response, click **Authorize** and paste it. Protected endpoints then work.

								Every error uses the same JSON shape: `code`, `message`, `fieldErrors`, `timestamp`.
								"""))
				.components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.description("The accessToken returned by POST /api/v1/auth/login")));
	}

}
