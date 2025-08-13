package io.artur.interview.kanga.spread_ranking.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for Spring Cloud Contract tests.
 * Provides base configuration for contract tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
    "logging.level.io.artur.interview.kanga.spread_ranking=DEBUG"
})
public abstract class KangaContractTestBase {

    @BeforeEach
    public void setup() {
        // Base setup - can be extended by subclasses
    }
}