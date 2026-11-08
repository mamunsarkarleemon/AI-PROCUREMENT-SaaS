package com.saas.procurement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Starts the whole app against a throwaway PostgreSQL. Skipped automatically when Docker is not installed. */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "app.jwt.secret=test-only-secret-that-is-at-least-32-chars", "app.seed-demo-data=true" })
class ProcurementApplicationTests {

	@Test
	void contextLoads() {
	}

}
