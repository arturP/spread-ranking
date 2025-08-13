package io.artur.interview.kanga.spread_ranking.infrastructure.external;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.ExchangeApiException;
import io.artur.interview.kanga.spread_ranking.domain.model.MarketPair;
import io.artur.interview.kanga.spread_ranking.domain.model.OrderBook;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.config.KangaApiProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Contract tests for KangaApiClientOptimized using Spring Cloud Contract with WireMock.
 * These tests verify that the client correctly handles various API responses according to the contracts.
 */
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
    "kanga.api.base-url=http://localhost:${wiremock.server.port}",
    "kanga.api.operation-timeout=PT2S",
    "kanga.api.pipeline-timeout=PT5S", 
    "kanga.api.retry-count=1",
    "logging.level.io.artur.interview.kanga.spread_ranking=DEBUG"
})
class KangaApiClientOptimizedContractTest {

    private KangaApiClientOptimized kangaApiClient;
    private KangaApiProperties properties;
    private CircuitBreaker circuitBreaker;
    private Clock clock;

    @BeforeEach
    void setUp() {
        // Setup test properties
        properties = new KangaApiProperties();
        properties.setBaseUrl("http://localhost:" + System.getProperty("wiremock.server.port", "8080"));
        properties.setOperationTimeout(Duration.ofSeconds(2));
        properties.setPipelineTimeout(Duration.ofSeconds(5));
        properties.setRetryCount(1);

        // Setup circuit breaker for testing
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(2)
                .build();
        circuitBreaker = CircuitBreaker.of("test-circuit-breaker", config);

        // Setup clock
        clock = Clock.systemDefaultZone();

        // Create WebClient
        WebClient webClient = WebClient.builder().build();

        // Initialize the client under test
        kangaApiClient = new KangaApiClientOptimized(webClient, circuitBreaker, clock, properties);
    }

    @Nested
    @DisplayName("Market Pairs Endpoint Contract Tests")
    class MarketPairsContractTests {

        @Test
        @DisplayName("Should successfully fetch market pairs when API returns valid data")
        void shouldFetchMarketPairsSuccessfully() {
            // Given: API returns valid market pairs (contract: market_pairs_success)
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
                                    },
                                    {
                                        "ticker_id": "LTC_PLN",
                                        "base": "LTC",
                                        "target": "PLN"
                                    }
                                ]
                                """)));

            // When: Fetching market pairs
            List<MarketPair> result = kangaApiClient.getMarketPairs();

            // Then: Should return expected market pairs
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getTickerId()).isEqualTo("BTC_PLN");
            assertThat(result.get(0).getBaseCurrency()).isEqualTo("BTC");
            assertThat(result.get(0).getTargetCurrency()).isEqualTo("PLN");
            
            verify(getRequestedFor(urlEqualTo("/market/pairs")));
        }

        @Test
        @DisplayName("Should handle empty market pairs response")
        void shouldHandleEmptyMarketPairsResponse() {
            // Given: API returns empty array (contract: market_pairs_empty)
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
        @DisplayName("Should throw ExchangeApiException when API returns server error")
        void shouldThrowExceptionOnServerError() {
            // Given: API returns server error (contract: market_pairs_server_error)
            stubFor(get(urlEqualTo("/market/pairs"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "error": "Internal server error",
                                    "message": "Unable to process request"
                                }
                                """)));

            // When & Then: Should throw ExchangeApiException
            assertThatThrownBy(() -> kangaApiClient.getMarketPairs())
                    .isInstanceOf(ExchangeApiException.class)
                    .hasMessageContaining("Cannot fetch market pairs");
            
            verify(getRequestedFor(urlEqualTo("/market/pairs")));
        }
    }

    @Nested
    @DisplayName("Order Book Endpoint Contract Tests") 
    class OrderBookContractTests {

        @Test
        @DisplayName("Should successfully fetch order book when API returns valid data")
        void shouldFetchOrderBookSuccessfully() {
            // Given: API returns valid order book (contract: orderbook_success)
            stubFor(get(urlEqualTo("/market/orderbook/BTC_PLN"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "ticker_id": "BTC_PLN",
                                    "bids": [
                                        ["185000.00", "0.1"],
                                        ["184500.00", "0.2"],
                                        ["184000.00", "0.5"]
                                    ],
                                    "asks": [
                                        ["186000.00", "0.1"],
                                        ["186500.00", "0.2"],
                                        ["187000.00", "0.3"]
                                    ],
                                    "timestamp": 1641234567890
                                }
                                """)));

            // When: Fetching order book
            OrderBook result = kangaApiClient.getOrderBook("BTC_PLN");

            // Then: Should return expected order book data
            assertThat(result).isNotNull();
            assertThat(result.getMarketId()).isEqualTo("BTC_PLN");
            assertThat(result.getBestBidPrice()).isEqualTo(new BigDecimal("185000.00"));
            assertThat(result.getBestAskPrice()).isEqualTo(new BigDecimal("186000.00"));
            assertThat(result.isEmpty()).isFalse();
            
            verify(getRequestedFor(urlEqualTo("/market/orderbook/BTC_PLN")));
        }

        @Test
        @DisplayName("Should handle empty order book response")
        void shouldHandleEmptyOrderBookResponse() {
            // Given: API returns empty order book (contract: orderbook_empty)
            stubFor(get(urlEqualTo("/market/orderbook/UNKNOWN_PAIR"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "ticker_id": "UNKNOWN_PAIR",
                                    "bids": [],
                                    "asks": [],
                                    "timestamp": 1641234567890
                                }
                                """)));

            // When: Fetching order book for unknown pair
            OrderBook result = kangaApiClient.getOrderBook("UNKNOWN_PAIR");

            // Then: Should return empty order book
            assertThat(result).isNotNull();
            assertThat(result.getMarketId()).isEqualTo("UNKNOWN_PAIR");
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getBestBidPrice()).isNull();
            assertThat(result.getBestAskPrice()).isNull();
            
            verify(getRequestedFor(urlEqualTo("/market/orderbook/UNKNOWN_PAIR")));
        }

        @Test
        @DisplayName("Should return empty order book when API returns not found error")
        void shouldReturnEmptyOrderBookOnNotFound() {
            // Given: API returns not found error (contract: orderbook_not_found)
            stubFor(get(urlEqualTo("/market/orderbook/INVALID_PAIR"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "error": "Market not found",
                                    "message": "The requested market pair does not exist"
                                }
                                """)));

            // When: Fetching order book for invalid pair
            OrderBook result = kangaApiClient.getOrderBook("INVALID_PAIR");

            // Then: Should return empty order book (graceful degradation)
            assertThat(result).isNotNull();
            assertThat(result.getMarketId()).isEqualTo("INVALID_PAIR");
            assertThat(result.isEmpty()).isTrue();
            
            verify(getRequestedFor(urlEqualTo("/market/orderbook/INVALID_PAIR")));
        }

        @Test
        @DisplayName("Should validate input parameters")
        void shouldValidateInputParameters() {
            // When & Then: Should throw IllegalArgumentException for null market ID
            assertThatThrownBy(() -> kangaApiClient.getOrderBook(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Market ID cannot be null or empty");

            // When & Then: Should throw IllegalArgumentException for empty market ID
            assertThatThrownBy(() -> kangaApiClient.getOrderBook(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Market ID cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Bulk Order Book Contract Tests")
    class BulkOrderBookContractTests {

        @Test
        @DisplayName("Should successfully fetch multiple order books")
        void shouldFetchMultipleOrderBooksSuccessfully() {
            // Given: API returns valid order books for multiple markets
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
            List<String> marketIds = List.of("BTC_PLN", "ETH_PLN");
            Map<String, OrderBook> result = kangaApiClient.getOrderBooks(marketIds);

            // Then: Should return order books for all requested markets
            assertThat(result).hasSize(2);
            assertThat(result).containsKeys("BTC_PLN", "ETH_PLN");
            
            OrderBook btcOrderBook = result.get("BTC_PLN");
            assertThat(btcOrderBook.getBestBidPrice()).isEqualTo(new BigDecimal("185000.00"));
            assertThat(btcOrderBook.getBestAskPrice()).isEqualTo(new BigDecimal("186000.00"));
            
            OrderBook ethOrderBook = result.get("ETH_PLN");
            assertThat(ethOrderBook.getBestBidPrice()).isEqualTo(new BigDecimal("12500.00"));
            assertThat(ethOrderBook.getBestAskPrice()).isEqualTo(new BigDecimal("12600.00"));
            
            verify(getRequestedFor(urlEqualTo("/market/orderbook/BTC_PLN")));
            verify(getRequestedFor(urlEqualTo("/market/orderbook/ETH_PLN")));
        }

        @Test
        @DisplayName("Should handle empty market IDs list")
        void shouldHandleEmptyMarketIdsList() {
            // When: Fetching with empty list
            Map<String, OrderBook> result = kangaApiClient.getOrderBooks(List.of());

            // Then: Should return empty map
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle null market IDs list")
        void shouldHandleNullMarketIdsList() {
            // When: Fetching with null list
            Map<String, OrderBook> result = kangaApiClient.getOrderBooks(null);

            // Then: Should return empty map
            assertThat(result).isEmpty();
        }
    }
}