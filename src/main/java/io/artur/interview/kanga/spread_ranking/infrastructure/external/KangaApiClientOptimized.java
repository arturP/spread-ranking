package io.artur.interview.kanga.spread_ranking.infrastructure.external;

import io.artur.interview.kanga.spread_ranking.domain.ExchangeApiClient;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.ExchangeApiException;
import io.artur.interview.kanga.spread_ranking.domain.model.MarketPair;
import io.artur.interview.kanga.spread_ranking.domain.model.OrderBook;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.config.KangaApiProperties;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.dto.KangaMarketPairResponse;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.dto.KangaOrderBookResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toConcurrentMap;

@Slf4j
public class KangaApiClientOptimized implements ExchangeApiClient {

    private static final String MARKET_PAIRS_ENDPOINT = "/market/pairs";
    private static final String ORDERBOOK_ENDPOINT = "/market/orderbook/{market}";

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Clock clock;
    private final KangaApiProperties properties;

    public KangaApiClientOptimized(WebClient webClient, CircuitBreaker circuitBreaker, 
                                  Clock clock, KangaApiProperties properties) {
        this.webClient = webClient.mutate()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "SpreadRankingService/1.0")
                .build();
        this.circuitBreaker = circuitBreaker;
        this.clock = clock;
        this.properties = properties;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up KangaApiClient resources");
    }

    /**
     * Fetches all available market pairs
     */
    public List<MarketPair> getMarketPairs() {
        log.debug("Fetching market pairs from Kanga API");

        try {
            List<KangaMarketPairResponse> apiResponse = webClient
                    .get()
                    .uri(MARKET_PAIRS_ENDPOINT)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleApiError)
                    .bodyToFlux(KangaMarketPairResponse.class)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .timeout(properties.getOperationTimeout())
                    .retry(properties.getRetryCount())
                    .collectList()
                    .block(properties.getPipelineTimeout());

            if (apiResponse == null || apiResponse.isEmpty()) {
                log.warn("Received empty market pairs response from Kanga API");
                return List.of();
            }

            List<MarketPair> marketPairs = apiResponse.stream()
                    .map(KangaApiMapper::toDomainMarketPair)
                    .filter(Objects::nonNull)
                    .toList();

            log.info("Successfully fetched {} market pairs", marketPairs.size());
            return marketPairs;

        } catch (Exception ex) {
            log.error("Failed to fetch market pairs from Kanga API", ex);
            throw new ExchangeApiException("Cannot fetch market pairs", ex);
        }
    }

    /**
     * Fetches orderbook for specific market
     */
    public OrderBook getOrderBook(String marketId) {
        if (marketId == null || marketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Market ID cannot be null or empty");
        }
        
        log.debug("Fetching orderbook for market: {}", marketId);

        try {
            KangaOrderBookResponse apiResponse = webClient
                    .get()
                    .uri(ORDERBOOK_ENDPOINT, marketId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleApiError)
                    .bodyToMono(KangaOrderBookResponse.class)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .timeout(properties.getOperationTimeout())
                    .retry(properties.getRetryCount())
                    .block(properties.getPipelineTimeout());

            if (apiResponse == null) {
                log.warn("Received null orderbook response for market: {}", marketId);
                return OrderBook.empty(marketId, clock);
            }

            OrderBook domainOrderBook = KangaApiMapper.toDomainOrderBook(apiResponse, clock);

            log.debug("Successfully fetched orderbook for market: {} with {} bids and {} asks",
                    marketId, apiResponse.getBids().size(), apiResponse.getAsks().size());

            return domainOrderBook;

        } catch (Exception ex) {
            log.error("Failed to fetch orderbook for market: {}", marketId, ex);
            // Return empty orderbook instead of failing - allows system to continue
            return OrderBook.empty(marketId, clock);
        }
    }

    /**
     * Fetches orderbooks for multiple markets in parallel
     */
    public Map<String, OrderBook> getOrderBooks(List<String> marketIds) {
        if (marketIds == null || marketIds.isEmpty()) {
            log.warn("Market IDs list is null or empty");
            return Map.of();
        }
        
        log.info("Fetching orderbooks for {} markets", marketIds.size());

        Map<String, OrderBook> orderBooks = marketIds.parallelStream()
                .collect(toConcurrentMap(
                        marketId -> marketId,
                        this::getOrderBook
                ));

        long successfulFetches = orderBooks.values().stream()
                .mapToLong(orderBook -> orderBook.isEmpty() ? 0 : 1)
                .sum();

        log.info("Successfully fetched {}/{} orderbooks", successfulFetches, marketIds.size());

        return orderBooks;
    }

    private Mono<? extends Throwable> handleApiError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("Unknown error")
                .map(errorBody -> {
                    log.error("Kanga API error - Status: {}, Body: {}",
                            response.statusCode(), errorBody);
                    return new ExchangeApiException(
                            String.format("API call failed with status %s: %s", response.statusCode(), errorBody));
                });
    }
}
