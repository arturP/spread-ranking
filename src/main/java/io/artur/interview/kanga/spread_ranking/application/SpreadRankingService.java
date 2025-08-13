package io.artur.interview.kanga.spread_ranking.application;

import io.artur.interview.kanga.spread_ranking.domain.*;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.RankingNotAvailableException;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.SpreadCalculationException;
import io.artur.interview.kanga.spread_ranking.domain.model.*;
import io.artur.interview.kanga.spread_ranking.domain.repository.MarketDataRepository;
import io.artur.interview.kanga.spread_ranking.domain.repository.SpreadRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.artur.interview.kanga.spread_ranking.domain.model.SpreadCategory.*;
import static java.util.Comparator.comparing;
import java.util.Comparator;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpreadRankingService {

    private final ExchangeApiClient exchangeApiClient;
    private final MarketDataRepository marketDataRepository;
    private final SpreadRankingRepository spreadRankingRepository;
    private final SpreadCalculationService spreadCalculationService;
    private final Clock clock;

    public SpreadRanking calculateSpreadRanking() {
        try {
            return performSpreadRankingCalculation();
        } catch (Exception ex) {
            handleCalculationFailure(ex);
            throw new SpreadCalculationException("Cannot calculate ranking", ex);
        }
    }

    private SpreadRanking performSpreadRankingCalculation() {
        List<Market> markets = fetchAndStoreMarkets();
        List<Spread> spreads = calculateSpreads(markets);
        return groupAndSortSpreads(spreads);
    }

    private List<Market> fetchAndStoreMarkets() {
        List<Market> markets = fetchMarkets();
        
        if (!markets.isEmpty()) {
            marketDataRepository.saveAll(markets);
        }
        
        return markets;
    }

    private void handleCalculationFailure(Exception ex) {
        log.error("Failed to calculate ranking", ex);
        spreadRankingRepository.clear();
    }

    public void storeSpreadRanking(SpreadRanking spreadRanking) {
        spreadRankingRepository.storeSpreadRanking(spreadRanking);
    }

    public SpreadRanking getCurrentRanking() {
        return spreadRankingRepository.getCurrentSpreadRanking()
                .filter(spreadRanking -> !spreadRankingRepository.isRankingExpired())
                .orElseThrow(() -> new RankingNotAvailableException("Valid ranking unavailable. Call calculate method first."));
    }

    public boolean isRankingCurrent() {
        return spreadRankingRepository.hasValidSpreadRanking();
    }

    private List<Market> fetchMarkets() {
        log.info("Fetching Market data from the Exchange");
        List<MarketPair> marketPairs = exchangeApiClient.getMarketPairs();

        List<String> marketIds = marketPairs.stream()
                .map(MarketPair::getTickerId)
                .collect(toList());

        Map<String, OrderBook> orderBooks;
        if (!marketIds.isEmpty()) {
            log.info("Fetching order books from the Exchange");
            orderBooks = exchangeApiClient.getOrderBooks(marketIds);
        } else {
            orderBooks = Map.of();
        }

        return createMarketsWithPrices(marketPairs, orderBooks);
    }

    private List<Spread> calculateSpreads(List<Market> markets) {
        log.debug("Calculating spreads for {} markets", markets.size());
        if (markets.isEmpty()) {
            log.warn("No markets provided for spread calculation");
            return List.of();
        }

        List<Spread> spreads = markets.stream()
                .filter(Objects::nonNull)
                .map(this::calculateSpreadSafely)
                .toList();

        log.info("Successfully calculated {} spreads from {} markets", spreads.size(), markets.size());

        return spreads;
    }

    private SpreadRanking groupAndSortSpreads(List<Spread> spreads) {
        log.debug("Grouping and sorting {} spreads", spreads.size());

        if (spreads.isEmpty()) {
            log.warn("No spreads provided for grouping");
            return SpreadRanking.empty(clock);
        }

        Map<SpreadCategory, List<Spread>> spreadsByCategory = spreads.stream()
                .collect(groupingBy(Spread::category));

        List<Spread> group1 = createSortedGroup(
                spreadsByCategory.get(LOW_SPREAD),
                "Group 1 (less or equal to 2%)");

        List<Spread> group2 = createSortedGroup(
                spreadsByCategory.get(HIGH_SPREAD),
                "Group 2 (greater than 2%)");

        List<Spread> group3 = createSortedGroup(
                spreadsByCategory.get(UNKNOWN),
                "Group 3 (UNKNOWN)");

        SpreadRanking ranking = SpreadRanking.builder()
                .lowSpreadMarkets(group1)
                .highSpreadMarkets(group2)
                .unavailableMarkets(group3)
                .calculatedAt(Instant.now(clock))
                .build();

        log.info("Created ranking with {} total markets distributed across 3 groups", ranking.getTotalMarketsCount());

        return ranking;
    }

    private List<Market> createMarketsWithPrices(List<MarketPair> marketPairs, Map<String, OrderBook> orderBooks) {
        return marketPairs.stream()
                .map(pair -> {
                    OrderBook orderBook = orderBooks.get(pair.getTickerId());
                    return new Market(
                            pair.getTickerId(),
                            orderBook != null ? orderBook.getBestBidPrice() : null,
                            orderBook != null ? orderBook.getBestAskPrice() : null
                    );
                })
                .toList();
    }

    private Spread calculateSpreadSafely(Market market) {
        try {
            Spread spread = spreadCalculationService.calculateSpread(market);
            log.trace("Calculated spread for market {}: {}%",
                    market.tickerId(),
                    spread.percentage() != null ? spread.percentage() : "N/A");

            return spread;
        } catch (Exception ex) {
            log.warn("Failed to calculate spread for market {}: {}", market.tickerId(), ex.getMessage());
            return Spread.unknown(market.tickerId());
        }
    }

    private List<Spread> createSortedGroup(List<Spread> spreads, String groupName) {
        if (spreads == null || spreads.isEmpty()) {
            log.debug("No spreads for group {}", groupName);
            return List.of();
        }

        // Sort by percentage first (ascending for LOW_SPREAD, descending for HIGH_SPREAD),
        // then by market ID for consistent ordering
        Comparator<Spread> comparator;
        
        if (groupName.contains("Group 1")) {
            // LOW_SPREAD: Sort by percentage ascending (lowest spreads first), then by market ID
            comparator = Comparator
                    .comparing(Spread::percentage, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Spread::marketId, String.CASE_INSENSITIVE_ORDER);
        } else if (groupName.contains("Group 2")) {
            // HIGH_SPREAD: Sort by percentage ascending (lowest spreads first), then by market ID
            comparator = Comparator
                    .comparing(Spread::percentage, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Spread::marketId, String.CASE_INSENSITIVE_ORDER);
        } else {
            // UNKNOWN: Sort by market ID only (no percentage available)
            comparator = Comparator.comparing(Spread::marketId, String.CASE_INSENSITIVE_ORDER);
        }

        List<Spread> sortedSpreads = spreads.stream()
                .sorted(comparator)
                .toList();
                
        log.debug("Sorted {} spreads for group {} by percentage and market ID", 
                sortedSpreads.size(), groupName);
                
        return sortedSpreads;
    }
}
