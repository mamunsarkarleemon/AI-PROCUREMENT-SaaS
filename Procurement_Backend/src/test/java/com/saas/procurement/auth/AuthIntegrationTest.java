package com.saas.procurement.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.saas.procurement.TestcontainersConfiguration;
import com.saas.procurement.user.UserRepository;

/**
 * Full HTTP + security + PostgreSQL test of sign-up and login (blueprint Appendix I).
 * Runs against a throwaway PostgreSQL container; skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes-long")
@AutoConfigureMockMvc
class AuthIntegrationTest {

	@Autowired MockMvc mvc;
	@Autowired UserRepository userRepository;

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	private static String registerJson(String email, String password, String accountType) {
		return """
				{"fullName":"Test User","email":"%s","password":"%s","accountType":"%s",
				 "companyName":"Test Co","countryCode":"QA"}
				""".formatted(email, password, accountType);
	}

	private void register(String email, String accountType) throws Exception {
		mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerJson(email, "Secret123", accountType)))
				.andExpect(status().isCreated());
	}

	private String login(String email) throws Exception {
		String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(email)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	@Test
	void register_thenLogin_thenReadProfile() throws Exception {
		String email = uniqueEmail();

		mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerJson(email.toUpperCase(), "Secret123", "BUYER")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.platformRole").value("USER"))
				.andExpect(jsonPath("$.memberships[0].role").value("BUYER_OWNER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		assertThat(userRepository.findByEmail(email)).get()
				.satisfies(u -> assertThat(u.getPasswordHash()).startsWith("{bcrypt}"));

		String token = login(email);

		mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.memberships[0].companyName").value("Test Co"));
	}

	@Test
	void duplicateEmail_isConflict() throws Exception {
		String email = uniqueEmail();
		register(email, "BUYER");

		mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerJson(email, "Secret123", "SUPPLIER")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	void publicRegistration_cannotRequestAdmin() throws Exception {
		mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerJson(uniqueEmail(), "Secret123", "ADMIN")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void weakPassword_returnsFieldErrors() throws Exception {
		mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerJson(uniqueEmail(), "short", "BUYER")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
				.andExpect(header().exists("X-Trace-Id"));
	}

	@Test
	void wrongPassword_isUnauthorized() throws Exception {
		String email = uniqueEmail();
		register(email, "BUYER");

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"WrongPass1\"}".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void noToken_is401() throws Exception {
		mvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(header().string("WWW-Authenticate", startsWith("Bearer")));
	}

	@Test
	void tamperedToken_is401() throws Exception {
		String email = uniqueEmail();
		register(email, "BUYER");
		String token = login(email);
		String tampered = token.substring(0, token.length() - 4) + "abcd";

		mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tampered))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void wrongRole_is403() throws Exception {
		String email = uniqueEmail();
		register(email, "BUYER");
		String token = login(email);

		mvc.perform(get("/api/v1/buyer/dashboard").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/supplier/dashboard").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

}
