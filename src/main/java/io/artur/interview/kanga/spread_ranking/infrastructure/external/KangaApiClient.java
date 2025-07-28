package io.artur.interview.kanga.spread_ranking.infrastructure.external;

import io.artur.interview.kanga.spread_ranking.domain.ExchangeApiClient;
import io.artur.interview.kanga.spread_ranking.domain.model.MarketPair;
import io.artur.interview.kanga.spread_ranking.domain.model.OrderBook;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.ExchangeApiException;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.dto.KangaMarketPairResponse;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.dto.KangaOrderBookResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Client for communication with Kanga exchange
 */
@Slf4j
@RequiredArgsConstructor
public class KangaApiClient implements ExchangeApiClient {

    private static final String KANGA_BASE_URL = "https://public.kanga.exchange/api";
    private static final String MARKET_PAIRS_ENDPOINT = "/market/pairs";
    private static final String ORDERBOOK_ENDPOINT = "/market/orderbook/{market}";
    private static final long MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final RestTemplate restTemplate;
    private final Clock clock;

    public List<MarketPair> getMarketPairs() {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Fetching market pairs - attempt {}/{}", attempt, MAX_RETRIES);

                String url = KANGA_BASE_URL + MARKET_PAIRS_ENDPOINT;
                KangaMarketPairResponse[] apiResponse = restTemplate.getForObject(
                        url,
                        KangaMarketPairResponse[].class
                );
                log.info("Response: {}", Arrays.toString(apiResponse));

                if (apiResponse == null || apiResponse.length == 0) {
                    log.warn("Received empty market pairs response");
                    return List.of();
                }

                List<MarketPair> marketPairs = Arrays.stream(apiResponse)
                        .map(KangaApiMapper::toDomainMarketPair)
                        .filter(Objects::nonNull)
                        .collect(toList());

                log.info("Successfully fetched {} market pairs on attempt {}", marketPairs.size(), attempt);
                return marketPairs;

            } catch (ResourceAccessException | HttpServerErrorException ex) {
                lastException = ex;
                log.warn("Attempt {}/{} failed: {}", attempt, MAX_RETRIES, ex.getMessage());

                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ExchangeApiException("Interrupted during retry", ie);
                }
            } catch (HttpClientErrorException ex) {
                // Don't retry client errors (4xx)
                log.error("Client error (no retry): {}", ex.getStatusCode());
                throw new ExchangeApiException("Client error: " + ex.getStatusCode(), ex);
            }
        }
        throw new ExchangeApiException("Failed to fetch market pairs after " + MAX_RETRIES + " attempts", lastException);
    }

    public Map<String, OrderBook> getOrderBooks(List<String> marketIds) {
        if (marketIds == null || marketIds.isEmpty()) {
            log.debug("No market IDs provided for order books");
            return Map.of();
        }

        log.debug("Fetching order books for {} markets", marketIds.size());
        Map<String, OrderBook> orderBooks = new HashMap<>();

        for (String marketId : marketIds) {
            Exception lastException = null;

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    log.trace("Fetching order book for market {} - attempt {}/{}", marketId, attempt, MAX_RETRIES);

                    String url = KANGA_BASE_URL + ORDERBOOK_ENDPOINT;
                    KangaOrderBookResponse apiResponse = restTemplate.getForObject(
                            url,
                            KangaOrderBookResponse.class,
                            marketId
                    );

                    if (apiResponse != null) {
                        OrderBook orderBook = KangaApiMapper.toDomainOrderBook(apiResponse, clock);
                        orderBooks.put(marketId, orderBook);
                        log.trace("Successfully fetched order book for market {} on attempt {}", marketId, attempt);
                        break;
                    } else {
                        log.warn("Received null order book response for market {}", marketId);
                        orderBooks.put(marketId, OrderBook.empty(marketId, clock));
                        break;
                    }

                } catch (ResourceAccessException | HttpServerErrorException ex) {
                    lastException = ex;
                    log.warn("Attempt {}/{} failed for market {}: {}", attempt, MAX_RETRIES, marketId, ex.getMessage());

                    if (attempt < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new ExchangeApiException("Interrupted during retry", ie);
                        }
                    }
                } catch (HttpClientErrorException ex) {
                    log.warn("Client error for market {} (no retry): {}", marketId, ex.getStatusCode());
                    break;
                }
            }

            if (lastException != null && !orderBooks.containsKey(marketId)) {
                log.warn("Failed to fetch order book for market {} after {} attempts", marketId, MAX_RETRIES);
            }
        }

        log.info("Successfully fetched {} out of {} order books", orderBooks.size(), marketIds.size());
        return orderBooks;
    }
}
