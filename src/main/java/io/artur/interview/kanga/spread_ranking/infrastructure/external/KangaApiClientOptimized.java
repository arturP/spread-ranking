package io.artur.interview.kanga.spread_ranking.infrastructure.external;

import io.artur.interview.kanga.spread_ranking.domain.ExchangeApiClient;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.ExchangeApiException;
import io.artur.interview.kanga.spread_ranking.domain.model.MarketPair;
import io.artur.interview.kanga.spread_ranking.domain.model.OrderBook;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.dto.KangaMarketPairResponse;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.dto.KangaOrderBookResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toConcurrentMap;

@Slf4j
public class KangaApiClientOptimized implements ExchangeApiClient {

    private static final String KANGA_BASE_URL = "https://public.kanga.exchange/api";
    private static final String MARKET_PAIRS_ENDPOINT = "/market/pairs";
    private static final String ORDERBOOK_ENDPOINT = "/market/orderbook/{market}";
    private static final int NUMBER_OF_RETRIES = 3;
    private static final int OPERATION_TIMEOUT_SEC = 10;
    private static final int PIPELINE_TIMEOUT_SEC = 30;

    private final WebClient webClient;
    private final Clock clock;

    public KangaApiClientOptimized(WebClient.Builder webClientBuilder, Clock clock) {
        this.webClient = webClientBuilder
                .baseUrl(KANGA_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "SpreadRankingService/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
        this.clock = clock;
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
                    .timeout(Duration.ofSeconds(OPERATION_TIMEOUT_SEC))
                    .retry(NUMBER_OF_RETRIES)
                    .collectList()
                    .block(Duration.ofSeconds(PIPELINE_TIMEOUT_SEC));

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
        log.debug("Fetching orderbook for market: {}", marketId);

        try {
            KangaOrderBookResponse apiResponse = webClient
                    .get()
                    .uri(ORDERBOOK_ENDPOINT, marketId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleApiError)
                    .bodyToMono(KangaOrderBookResponse.class)
                    .timeout(Duration.ofSeconds(OPERATION_TIMEOUT_SEC))
                    .retry(NUMBER_OF_RETRIES)
                    .block(Duration.ofSeconds(PIPELINE_TIMEOUT_SEC));

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
