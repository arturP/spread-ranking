package io.artur.interview.kanga.spread_ranking.infrastructure.external;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.ExchangeApiException;
import io.artur.interview.kanga.spread_ranking.domain.model.MarketPair;
import io.artur.interview.kanga.spread_ranking.domain.model.OrderBook;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.config.KangaApiProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Simplified contract tests for KangaApiClientOptimized using WireMock directly.
 * These tests verify client behavior against mocked API responses.
 */
class KangaApiClientSimpleContractTest {

    private WireMockServer wireMockServer;
    private KangaApiClientOptimized kangaApiClient;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .port(0)); // Use random available port
        wireMockServer.start();

        // Configure WireMock client
        configureFor("localhost", wireMockServer.port());

        // Setup test properties
        KangaApiProperties properties = new KangaApiProperties();
        properties.setBaseUrl("http://localhost:" + wireMockServer.port());
        properties.setOperationTimeout(Duration.ofSeconds(2));
        properties.setPipelineTimeout(Duration.ofSeconds(5));
        properties.setRetryCount(1);

        // Setup circuit breaker
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(2)
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test-circuit-breaker", config);

        // Create client
        WebClient webClient = WebClient.builder().build();
        kangaApiClient = new KangaApiClientOptimized(webClient, circuitBreaker, 
                Clock.systemDefaultZone(), properties);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Nested
    @DisplayName("Market Pairs Contract Tests")
    class MarketPairsTests {

        @Test
        @DisplayName("Should successfully fetch market pairs")
        void shouldFetchMarketPairsSuccessfully() {
            // Given: Mock successful response
            stubFor(get(urlEqualTo("/market/pairs"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                [
                                    {
                                        "ticker_id": "BTC_PLN",
                                        "base": "BTC",
                                        "target": "PLN"
                                    },
                                    {
                                        "ticker_id": "ETH_PLN",
                                        "base": "ETH", 
                                        "target": "PLN"
                                    }
                                ]
                                """)));

            // When: Fetching market pairs
            List<MarketPair> result = kangaApiClient.getMarketPairs();

            // Then: Should return expected data
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTickerId()).isEqualTo("BTC_PLN");
            assertThat(result.get(0).getBaseCurrency()).isEqualTo("BTC");
            assertThat(result.get(0).getTargetCurrency()).isEqualTo("PLN");

            // Verify request was made
            verify(getRequestedFor(urlEqualTo("/market/pairs")));
        }

        @Test
        @DisplayName("Should handle empty market pairs response")
        void shouldHandleEmptyResponse() {
            // Given: Empty response
            stubFor(get(urlEqualTo("/market/pairs"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[]")));

            // When: Fetching market pairs
            List<MarketPair> result = kangaApiClient.getMarketPairs();

            // Then: Should return empty list
            assertThat(result).isEmpty();
            verify(getRequestedFor(urlEqualTo("/market/pairs")));
        }

        @Test
        @DisplayName("Should handle server error")
        void shouldHandleServerError() {
            // Given: Server error response
            stubFor(get(urlEqualTo("/market/pairs"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "error": "Internal server error"
                                }
                                """)));

            // When & Then: Should throw exception
            assertThatThrownBy(() -> kangaApiClient.getMarketPairs())
                    .isInstanceOf(ExchangeApiException.class)
                    .hasMessageContaining("Cannot fetch market pairs");

            verify(getRequestedFor(urlEqualTo("/market/pairs")));
        }
    }

    @Nested
    @DisplayName("Order Book Contract Tests")
    class OrderBookTests {

        @Test
        @DisplayName("Should successfully fetch order book")
        void shouldFetchOrderBookSuccessfully() {
            // Given: Valid order book response
            stubFor(get(urlEqualTo("/market/orderbook/BTC_PLN"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "ticker_id": "BTC_PLN",
                                    "bids": [["185000.00", "0.1"]],
                                    "asks": [["186000.00", "0.1"]],
                                    "timestamp": 1641234567890
                                }
                                """)));

            // When: Fetching order book
            OrderBook result = kangaApiClient.getOrderBook("BTC_PLN");

            // Then: Should return valid data
            assertThat(result).isNotNull();
            assertThat(result.getMarketId()).isEqualTo("BTC_PLN");
            assertThat(result.getBestBidPrice()).isEqualTo(new BigDecimal("185000.00"));
            assertThat(result.getBestAskPrice()).isEqualTo(new BigDecimal("186000.00"));
            assertThat(result.isEmpty()).isFalse();

            verify(getRequestedFor(urlEqualTo("/market/orderbook/BTC_PLN")));
        }

        @Test
        @DisplayName("Should handle empty order book")
        void shouldHandleEmptyOrderBook() {
            // Given: Empty order book response
            stubFor(get(urlEqualTo("/market/orderbook/EMPTY_PAIR"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "ticker_id": "EMPTY_PAIR",
                                    "bids": [],
                                    "asks": [],
                                    "timestamp": 1641234567890
                                }
                                """)));

            // When: Fetching order book
            OrderBook result = kangaApiClient.getOrderBook("EMPTY_PAIR");

            // Then: Should return empty order book
            assertThat(result).isNotNull();
            assertThat(result.getMarketId()).isEqualTo("EMPTY_PAIR");
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getBestBidPrice()).isNull();
            assertThat(result.getBestAskPrice()).isNull();

            verify(getRequestedFor(urlEqualTo("/market/orderbook/EMPTY_PAIR")));
        }

        @Test
        @DisplayName("Should handle not found error gracefully")
        void shouldHandleNotFoundError() {
            // Given: 404 error response
            stubFor(get(urlEqualTo("/market/orderbook/NOT_FOUND"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "error": "Market not found"
                                }
                                """)));

            // When: Fetching non-existent order book
            OrderBook result = kangaApiClient.getOrderBook("NOT_FOUND");

            // Then: Should return empty order book (graceful degradation)
            assertThat(result).isNotNull();
            assertThat(result.getMarketId()).isEqualTo("NOT_FOUND");
            assertThat(result.isEmpty()).isTrue();

            verify(getRequestedFor(urlEqualTo("/market/orderbook/NOT_FOUND")));
        }

        @Test
        @DisplayName("Should validate input parameters")
        void shouldValidateInputParameters() {
            // When & Then: Should throw exception for invalid inputs
            assertThatThrownBy(() -> kangaApiClient.getOrderBook(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Market ID cannot be null or empty");

            assertThatThrownBy(() -> kangaApiClient.getOrderBook(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Market ID cannot be null or empty");

            assertThatThrownBy(() -> kangaApiClient.getOrderBook("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Market ID cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Bulk Order Book Tests")
    class BulkOrderBookTests {

        @Test
        @DisplayName("Should fetch multiple order books")
        void shouldFetchMultipleOrderBooks() {
            // Given: Multiple valid responses
            stubFor(get(urlEqualTo("/market/orderbook/BTC_PLN"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "ticker_id": "BTC_PLN",
                                    "bids": [["185000.00", "0.1"]],
                                    "asks": [["186000.00", "0.1"]],
                                    "timestamp": 1641234567890
                                }
                                """)));

            stubFor(get(urlEqualTo("/market/orderbook/ETH_PLN"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "ticker_id": "ETH_PLN", 
                                    "bids": [["12500.00", "1.0"]],
                                    "asks": [["12600.00", "1.5"]],
                                    "timestamp": 1641234567890
                                }
                                """)));

            // When: Fetching multiple order books
            Map<String, OrderBook> result = kangaApiClient.getOrderBooks(List.of("BTC_PLN", "ETH_PLN"));

            // Then: Should return both order books
            assertThat(result).hasSize(2);
            assertThat(result).containsKeys("BTC_PLN", "ETH_PLN");

            OrderBook btc = result.get("BTC_PLN");
            assertThat(btc.getBestBidPrice()).isEqualTo(new BigDecimal("185000.00"));

            OrderBook eth = result.get("ETH_PLN");
            assertThat(eth.getBestBidPrice()).isEqualTo(new BigDecimal("12500.00"));

            verify(getRequestedFor(urlEqualTo("/market/orderbook/BTC_PLN")));
            verify(getRequestedFor(urlEqualTo("/market/orderbook/ETH_PLN")));
        }

        @Test
        @DisplayName("Should handle empty input lists")
        void shouldHandleEmptyInputLists() {
            // When & Then: Empty lists should return empty results
            assertThat(kangaApiClient.getOrderBooks(List.of())).isEmpty();
            assertThat(kangaApiClient.getOrderBooks(null)).isEmpty();
        }
    }
}