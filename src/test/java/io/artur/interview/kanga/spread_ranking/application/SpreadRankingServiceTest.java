package io.artur.interview.kanga.spread_ranking.application;

import io.artur.interview.kanga.spread_ranking.domain.*;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.SpreadCalculationException;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.ExchangeApiException;
import io.artur.interview.kanga.spread_ranking.domain.model.*;
import io.artur.interview.kanga.spread_ranking.domain.repository.MarketDataRepository;
import io.artur.interview.kanga.spread_ranking.domain.repository.SpreadRankingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.artur.interview.kanga.spread_ranking.domain.model.SpreadCategory.LOW_SPREAD;
import static io.artur.interview.kanga.spread_ranking.domain.model.SpreadCategory.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SpreadRankingServiceTest {

    @Mock
    private ExchangeApiClient exchangeApiClient;
    @Mock
    private MarketDataRepository marketDataRepository;
    @Mock
    private SpreadCalculationService spreadCalculationService;
    @Mock
    private SpreadRankingRepository spreadRankingRepository;
    @Mock
    private Clock clock;
    @InjectMocks
    private SpreadRankingService spreadRankingService;
    @Captor
    private ArgumentCaptor<List<Market>> marketsCaptor;

    @Test
    void shouldCalculateAndCacheRankingSuccessfully() {
        // given
        List<MarketPair> marketPairs = List.of(
                createMarketPair("BTC_USD", "BTC", "USD"),
                createMarketPair("ETH_USD", "ETH", "USD"),
                createMarketPair("ADA_USD", "ADA", "USD")
        );

        Map<String, OrderBook> orderBooks = Map.of(
                "BTC_USD", createOrderBook("BTC_USD", "50000", "50100", clock),  // Low spread
                "ETH_USD", createOrderBook("ETH_USD", "3000", "3200", clock),    // High spread
                "ADA_USD", createOrderBook("ADA_USD", null, "1.50", clock)       // Missing bid
        );

        List<Spread> expectedSpreads = List.of(
                createSpread("BTC_USD", "0.20", SpreadCategory.LOW_SPREAD),
                createSpread("ETH_USD", "6.45", SpreadCategory.HIGH_SPREAD),
                createSpread("ADA_USD", "0", UNKNOWN)
        );
        Instant testTime = Instant.parse("2025-07-20T10:30:00Z");

        when(clock.instant()).thenReturn(testTime);
        when(exchangeApiClient.getMarketPairs()).thenReturn(marketPairs);
        when(exchangeApiClient.getOrderBooks(anyList())).thenReturn(orderBooks);
        when(spreadCalculationService.calculateSpread(any(Market.class)))
                .thenReturn(expectedSpreads.get(0))
                .thenReturn(expectedSpreads.get(1))
                .thenReturn(expectedSpreads.get(2));

        // when
        SpreadRanking result = spreadRankingService.calculateSpreadRanking();

        // then
        assertNotNull(result);
        assertThat(result.getLowSpreadMarkets()).hasSize(1);
        assertThat(result.getLowSpreadMarkets().getFirst()).isEqualTo(expectedSpreads.getFirst());
        assertThat(result.getHighSpreadMarkets()).hasSize(1);
        assertThat(result.getHighSpreadMarkets().getFirst()).isEqualTo(expectedSpreads.get(1));
        assertThat(result.getUnavailableMarkets()).hasSize(1);
        assertThat(result.getUnavailableMarkets().getFirst()).isEqualTo(expectedSpreads.get(2));
        assertThat(result.getTotalMarketsCount()).isEqualTo(3);
        assertThat(result.getCalculatedAt()).isEqualTo(testTime);

        verify(exchangeApiClient).getMarketPairs();
        verify(exchangeApiClient).getOrderBooks(List.of("BTC_USD", "ETH_USD", "ADA_USD"));
        verify(marketDataRepository).saveAll(marketsCaptor.capture());

        List<Market> savedMarkets = marketsCaptor.getValue();
        assertThat(savedMarkets).hasSize(3);
        assertThat(savedMarkets.getFirst().tickerId()).isEqualTo("BTC_USD");

        verify(spreadCalculationService, times(3)).calculateSpread(any(Market.class));

        verifyNoMoreInteractions(marketDataRepository, exchangeApiClient, spreadCalculationService);
        verifyNoInteractions(spreadRankingRepository);
    }

    @Test
    void shouldHandleEmptyMarketPairs() {
        // given
        Instant testTime = Instant.parse("2025-07-20T12:55:00Z");

        when(clock.instant()).thenReturn(testTime);
        when(exchangeApiClient.getMarketPairs()).thenReturn(List.of());

        // when
        SpreadRanking result = spreadRankingService.calculateSpreadRanking();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLowSpreadMarkets()).isEmpty();
        assertThat(result.getHighSpreadMarkets()).isEmpty();
        assertThat(result.getUnavailableMarkets()).isEmpty();
        assertThat(result.getTotalMarketsCount()).isEqualTo(0);
        assertThat(result.getCalculatedAt()).isEqualTo(testTime);

        verify(exchangeApiClient).getMarketPairs();
        verify(exchangeApiClient, never()).getOrderBooks(anyList());
        verifyNoMoreInteractions(exchangeApiClient);
        verifyNoInteractions(marketDataRepository, spreadRankingRepository, spreadCalculationService);
    }

    @Test
    void shouldSortMarketsAlphabetically() {
        // given
        List<MarketPair> marketPairs = List.of(
                createMarketPair("ZEC_EUR", "ZEC", "EUR"),
                createMarketPair("ADA_GBP", "ADA", "GBP"),
                createMarketPair("BTC_USD", "BCT", "USD"));

        Map<String, OrderBook> orderBooks = Map.of(
                "ZEC_EUR", createOrderBook("ZEC_EUR", "100", "101", clock),
                "ADA_GBP", createOrderBook("ADA_GBP", "1", "1.01", clock),
                "BTC_USD", createOrderBook("BTC_USD", "100000", "100100", clock)
        );
        Instant testTime = Instant.parse("2025-07-20T12:55:00Z");

        when(clock.instant()).thenReturn(testTime);
        when(exchangeApiClient.getMarketPairs()).thenReturn(marketPairs);
        when(exchangeApiClient.getOrderBooks(anyList())).thenReturn(orderBooks);
        when(spreadCalculationService.calculateSpread(any(Market.class)))
                .thenReturn(createSpread("ZEC_EUR", "1.25", LOW_SPREAD))
                .thenReturn(createSpread("ADA_GBP", "1.01", LOW_SPREAD))
                .thenReturn(createSpread("BTC_USD", "0.99", LOW_SPREAD));

        // when
        SpreadRanking result = spreadRankingService.calculateSpreadRanking();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLowSpreadMarkets())
                .extracting(Spread::marketId)
                .containsExactly("BTC_USD", "ADA_GBP", "ZEC_EUR");
    }

    @Test
    void shouldHandleExchangeApiClientFailure() {
        // given
        when(exchangeApiClient.getMarketPairs()).thenThrow(new ExchangeApiException("Api unavailable", new RuntimeException()));

        // when
        assertThatThrownBy(() -> spreadRankingService.calculateSpreadRanking())
                .isInstanceOf(SpreadCalculationException.class)
                .hasMessageContaining("Cannot calculate ranking")
                .hasCauseInstanceOf(ExchangeApiException.class);
    }

    @Test
    void shouldHandleOrderBookFailure() {
        // given
        List<MarketPair> marketPairs = List.of(createMarketPair("BTC_USD", "BTC", "USD"));

        when(exchangeApiClient.getMarketPairs()).thenReturn(marketPairs);
        when(exchangeApiClient.getOrderBooks(anyList())).thenThrow(new ExchangeApiException("OrderBook API failed", new RuntimeException()));

        // when
        assertThatThrownBy(() -> spreadRankingService.calculateSpreadRanking())
                .isInstanceOf(SpreadCalculationException.class);
        verify(spreadRankingRepository).clear();
    }

    private MarketPair createMarketPair(String tickerId, String baseCurrency, String targetCurrency) {
        return new MarketPair(tickerId, baseCurrency, targetCurrency);
    }

    private OrderBook createOrderBook(String marketId, String bidPrice, String askPrice, Clock clock) {
        return new OrderBook(
                marketId,
                bidPrice != null ? new BigDecimal(bidPrice) : null,
                askPrice != null ? new BigDecimal(askPrice) : null,
                Instant.now(clock)
        );
    }

    private Spread createSpread(String marketId, String percentage, SpreadCategory category) {
        if (category == UNKNOWN) {
            return Spread.unknown(marketId);
        }
        return new Spread(marketId, new BigDecimal(percentage), category);
    }
}