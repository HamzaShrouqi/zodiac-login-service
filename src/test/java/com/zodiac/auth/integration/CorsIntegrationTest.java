package com.zodiac.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zodiac.auth.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CORS: only the websites listed in configuration may call the API from a browser.
 *
 * <p>Before a real POST, a browser sends a "preflight" OPTIONS request asking
 * "may a page from origin X send this kind of request?". We check the server's answer.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "zodiac.cors.allowed-origins=http://localhost:5500")
@AutoConfigureMockMvc
class CorsIntegrationTest {

	private static final String LOGIN = "/api/v1/auth/login";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void preflightFromTheListedWebsiteIsApproved() throws Exception {
		mockMvc.perform(options(LOGIN)
				.header(HttpHeaders.ORIGIN, "http://localhost:5500")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
						org.hamcrest.Matchers.containsString("POST")));
	}

	@Test
	void preflightFromAnyOtherWebsiteIsRefused() throws Exception {
		mockMvc.perform(options(LOGIN)
				.header(HttpHeaders.ORIGIN, "http://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void realRequestsFromTheListedWebsiteGetTheApprovalHeader() throws Exception {
		mockMvc.perform(post(LOGIN)
				.header(HttpHeaders.ORIGIN, "http://localhost:5500")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"nobody@zodiac.test\",\"password\":\"whatever-123\"}"))
				.andExpect(status().isUnauthorized()) // wrong login, but...
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"));
	}

}
