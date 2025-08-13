package io.artur.interview.kanga.spread_ranking.infrastructure.external;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for Spring Cloud Contract tests.
 * Sets up WireMock server to simulate Kanga API responses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
    "kanga.api.base-url=http://localhost:${wiremock.server.port}",
    "logging.level.io.artur.interview.kanga.spread_ranking=DEBUG"
})
public abstract class KangaContractTestBase {

    protected WireMockServer wireMockServer;

    @BeforeEach
    public void setup() {
        // Start WireMock server on a random port
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .port(0) // Use random available port
                .usingFilesUnderDirectory("src/test/resources"));
        
        wireMockServer.start();
        
        // Set the port system property for use in @TestPropertySource
        System.setProperty("wiremock.server.port", String.valueOf(wireMockServer.port()));
        
        // Configure RestAssured to use MockMvc
        RestAssuredMockMvc.standaloneSetup(new KangaApiStubController());
    }

    @AfterEach
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    /**
     * Mock controller that simulates Kanga API endpoints for contract testing.
     * This controller implements the contract definitions defined in the .groovy files.
     */
    public static class KangaApiStubController {
        // Spring Cloud Contract will generate the actual implementation
        // based on the contract definitions in src/test/resources/contracts/kanga/
    }
}