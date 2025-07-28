package io.artur.interview.kanga.spread_ranking.api;

import io.artur.interview.kanga.spread_ranking.application.SpreadRankingService;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.RankingNotAvailableException;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.SpreadCalculationException;
import io.artur.interview.kanga.spread_ranking.domain.model.Spread;
import io.artur.interview.kanga.spread_ranking.domain.model.SpreadCategory;
import io.artur.interview.kanga.spread_ranking.domain.model.SpreadRanking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "app.security.api-token=TEST_INTEGRATION_TOKEN",
    "logging.level.io.artur.interview.kanga.spread_ranking=DEBUG"
})
class SpreadControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private SpreadRankingService spreadRankingService;

    @MockBean
    private Clock clock;

    private SpreadRanking mockSpreadRanking;
    private final Instant fixedInstant = Instant.parse("2023-01-01T12:00:00Z");
    private String baseUrl;
    private HttpHeaders authenticatedHeaders;
    private HttpHeaders invalidTokenHeaders;
    private HttpHeaders noAuthHeaders;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        
        // Setup clock mock
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // Create mock spread ranking
        List<Spread> lowSpreadMarkets = List.of(
                new Spread("BTC-PLN", new BigDecimal("1.5"), SpreadCategory.LOW_SPREAD),
                new Spread("ETH-PLN", new BigDecimal("1.8"), SpreadCategory.LOW_SPREAD)
        );
        List<Spread> highSpreadMarkets = List.of(
                new Spread("DOGE-PLN", new BigDecimal("3.2"), SpreadCategory.HIGH_SPREAD)
        );
        List<Spread> unavailableMarkets = List.of(
                Spread.unknown("ADA-PLN")
        );

        mockSpreadRanking = SpreadRanking.builder()
                .lowSpreadMarkets(lowSpreadMarkets)
                .highSpreadMarkets(highSpreadMarkets)
                .unavailableMarkets(unavailableMarkets)
                .calculatedAt(fixedInstant)
                .build();

        // Setup headers
        authenticatedHeaders = new HttpHeaders();
        authenticatedHeaders.set("Authorization", "Bearer TEST_INTEGRATION_TOKEN");
        authenticatedHeaders.setContentType(MediaType.APPLICATION_JSON);

        invalidTokenHeaders = new HttpHeaders();
        invalidTokenHeaders.set("Authorization", "Bearer INVALID_TOKEN");
        invalidTokenHeaders.setContentType(MediaType.APPLICATION_JSON);

        noAuthHeaders = new HttpHeaders();
        noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    // Authentication and Authorization Tests

    @Test
    void getRanking_shouldReturn401_whenNoAuthorizationHeader() {
        HttpEntity<String> entity = new HttpEntity<>(noAuthHeaders);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/ranking",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getRanking_shouldReturn401_whenInvalidBearerToken() {
        HttpEntity<String> entity = new HttpEntity<>(invalidTokenHeaders);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/ranking",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void calculateRanking_shouldReturn401_whenNoAuthorizationHeader() {
        HttpEntity<String> entity = new HttpEntity<>(noAuthHeaders);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/calculate",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void calculateRanking_shouldReturn401_whenInvalidBearerToken() {
        HttpEntity<String> entity = new HttpEntity<>(invalidTokenHeaders);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/calculate",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // Successful Request Tests

    @Test
    void getRanking_shouldReturnCurrentRanking_whenValidToken() {
        // Given
        when(spreadRankingService.isRankingCurrent()).thenReturn(true);
        when(spreadRankingService.getCurrentRanking()).thenReturn(mockSpreadRanking);

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/ranking",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        
        String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("\"timestamp\":");
        assertThat(responseBody).contains("\"ranking\":");
        assertThat(responseBody).contains("\"group1\":");
        assertThat(responseBody).contains("\"group2\":");
        assertThat(responseBody).contains("\"group3\":");
        assertThat(responseBody).contains("BTC-PLN");
        assertThat(responseBody).contains("1.5");

        verify(spreadRankingService).isRankingCurrent();
        verify(spreadRankingService).getCurrentRanking();
    }

    @Test
    void getRanking_shouldCalculateNewRanking_whenCurrentRankingIsOutdated() {
        // Given
        when(spreadRankingService.isRankingCurrent()).thenReturn(false);
        when(spreadRankingService.calculateSpreadRanking()).thenReturn(mockSpreadRanking);

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/ranking",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        
        String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("BTC-PLN");
        assertThat(responseBody).contains("DOGE-PLN");
        assertThat(responseBody).contains("ADA-PLN");

        verify(spreadRankingService).isRankingCurrent();
        verify(spreadRankingService).calculateSpreadRanking();
        verify(spreadRankingService).storeSpreadRanking(mockSpreadRanking);
    }

    @Test
    void calculateRanking_shouldReturnSuccess_whenValidToken() {
        // Given
        when(spreadRankingService.calculateSpreadRanking()).thenReturn(mockSpreadRanking);

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/calculate",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        
        String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("\"message\":\"Ranking calculated successfully\"");
        assertThat(responseBody).contains("\"status\":\"SUCCESS\"");
        assertThat(responseBody).contains("\"timestamp\":");

        verify(spreadRankingService).calculateSpreadRanking();
        verify(spreadRankingService).storeSpreadRanking(mockSpreadRanking);
    }

    @Test
    void getRanking_shouldReturn404_whenRankingNotAvailable() {
        // Given
        when(spreadRankingService.isRankingCurrent()).thenReturn(true);
        when(spreadRankingService.getCurrentRanking())
                .thenThrow(new RankingNotAvailableException("No ranking data available"));

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/ranking",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(spreadRankingService).isRankingCurrent();
        verify(spreadRankingService).getCurrentRanking();
    }

    @Test
    void getRanking_shouldReturn500_whenSpreadCalculationFails() {
        // Given
        when(spreadRankingService.isRankingCurrent()).thenReturn(false);
        when(spreadRankingService.calculateSpreadRanking())
                .thenThrow(new SpreadCalculationException("External API failure", new RuntimeException()));

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/ranking",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(spreadRankingService).isRankingCurrent();
        verify(spreadRankingService).calculateSpreadRanking();
    }

    @Test
    void calculateRanking_shouldReturnError_whenSpreadCalculationFails() {
        // Given
        String errorMessage = "Market data unavailable";
        when(spreadRankingService.calculateSpreadRanking())
                .thenThrow(new SpreadCalculationException(errorMessage, new RuntimeException()));

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/calculate",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        
        String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("\"message\":\"Calculation failed: " + errorMessage + "\"");
        assertThat(responseBody).contains("\"status\":\"ERROR\"");

        verify(spreadRankingService).calculateSpreadRanking();
        verify(spreadRankingService, never()).storeSpreadRanking(any());
    }

    @Test
    void getRanking_shouldHandleMalformedAuthorizationHeader() {
        HttpHeaders malformedHeaders = new HttpHeaders();
        malformedHeaders.set("Authorization", "Invalid MALFORMED_TOKEN");
        malformedHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(malformedHeaders);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/ranking",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void calculateRanking_shouldHandleEmptyAuthorizationHeader() {
        HttpHeaders emptyAuthHeaders = new HttpHeaders();
        emptyAuthHeaders.set("Authorization", "");
        emptyAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(emptyAuthHeaders);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/calculate",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getRanking_shouldCompleteWithinReasonableTime() {
        // Given
        when(spreadRankingService.isRankingCurrent()).thenReturn(true);
        when(spreadRankingService.getCurrentRanking()).thenReturn(mockSpreadRanking);

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);

        // When
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/ranking",
                HttpMethod.GET,
                entity,
                String.class
        );
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds
    }

    @Test
    void calculateRanking_shouldCompleteWithinReasonableTime() {
        // Given
        when(spreadRankingService.calculateSpreadRanking()).thenReturn(mockSpreadRanking);

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);

        // When
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/spread/calculate",
                HttpMethod.POST,
                entity,
                String.class
        );
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(endTime - startTime).isLessThan(10000);
        
        String responseBody = response.getBody();
        assertThat(responseBody).contains("\"calculationDurationMs\":");
    }

    @Test
    void shouldHandleConcurrentRequests() throws InterruptedException {
        // Given
        when(spreadRankingService.isRankingCurrent()).thenReturn(true);
        when(spreadRankingService.getCurrentRanking()).thenReturn(mockSpreadRanking);

        HttpEntity<String> entity = new HttpEntity<>(authenticatedHeaders);
        
        // When - Make 5 concurrent requests
        Thread[] threads = new Thread[5];
        ResponseEntity<String>[] responses = new ResponseEntity[5];
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                responses[index] = restTemplate.exchange(
                        baseUrl + "/api/spread/ranking",
                        HttpMethod.GET,
                        entity,
                        String.class
                );
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All requests should succeed
        for (ResponseEntity<String> response : responses) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("BTC-PLN");
        }
    }
}