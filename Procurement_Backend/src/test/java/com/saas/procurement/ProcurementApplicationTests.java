package com.saas.procurement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Skipped automatically when Docker is not available. */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes-long")
class ProcurementApplicationTests {

	@Test
	void contextLoads() {
	}

}
