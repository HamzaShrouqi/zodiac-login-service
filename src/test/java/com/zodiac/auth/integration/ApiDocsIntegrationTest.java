package com.zodiac.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zodiac.auth.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** The generated API description must list every endpoint, so the documentation cannot silently go stale. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
class ApiDocsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void describesEveryEndpoint() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Zodiac Login Service API"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/me'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/password'].put").exists());
	}

	@Test
	void protectedEndpointsAskForABearerToken() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist());
	}

	@Test
	void theAuthorizationHeaderIsNotListedAsAFormField() throws Exception {
		// It is handled by the "Authorize" button, so it must not also appear as a parameter on each endpoint.
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.parameters").doesNotExist());
	}

}
