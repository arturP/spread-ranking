package io.artur.interview.kanga.spread_ranking.domain;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static io.artur.interview.kanga.spread_ranking.domain.SpreadCategory.HIGH_SPREAD;
import static io.artur.interview.kanga.spread_ranking.domain.SpreadCategory.LOW_SPREAD;
import static org.assertj.core.api.Assertions.assertThat;

class SpreadCalculationServiceTest {

    private SpreadCalculationService service;

    @BeforeEach
    void setUp() {
        service = new SpreadCalculationService();
    }

    @Test
    void calculateSpreadExpectedLowSpread() {
        // given
        String tickerId = "EUR_PLN";
        BigDecimal askPrice = new BigDecimal("4.3102");
        BigDecimal bidPrice = new BigDecimal("4.2721");
        Market market = new Market(tickerId, bidPrice, askPrice);
        BigDecimal expectedSpreadPctValue = calculateSpreadPercentageValue(market);
        Spread expectedSpread = new Spread(tickerId, expectedSpreadPctValue, LOW_SPREAD);

        // when
        Spread currentSpread = service.calculateSpread(market);

        // then
        assertThat(currentSpread).isEqualTo(expectedSpread);
    }

    @Test
    void calculateSpreadExpectedHighSpread() {
        // given
        String tickerId = "EUR_PLN";
        BigDecimal askPrice = new BigDecimal("4.5997");
        BigDecimal bidPrice = new BigDecimal("4.2610");
        Market market = new Market(tickerId, bidPrice, askPrice);
        BigDecimal expectedSpreadPctValue = calculateSpreadPercentageValue(market);
        Spread expectedSpread = new Spread(tickerId, expectedSpreadPctValue, HIGH_SPREAD);

        // when
        Spread currentSpread = service.calculateSpread(market);

        // then
        assertThat(currentSpread).isEqualTo(expectedSpread);
    }

    @Test
    void calculateSpreadNoBidPriceExpectedUnknownSpread() {
        // given
        String tickerId = "EUR_PLN";
        BigDecimal askPrice = new BigDecimal("4.5997");
        Market market = new Market(tickerId, null, askPrice);
        Spread expectedSpread = Spread.unknown(tickerId);

        // when
        Spread currentSpread = service.calculateSpread(market);

        // then
        assertThat(currentSpread).isEqualTo(expectedSpread);

    }

    @Test
    void calculateSpreadNoAskPriceExpectedUnknownSpread() {
        // given
        String tickerId = "EUR_PLN";
        BigDecimal bidPrice = new BigDecimal("4.0013");
        Market market = new Market(tickerId, bidPrice, null);
        Spread expectedSpread = Spread.unknown(tickerId);

        // when
        Spread currentSpread = service.calculateSpread(market);

        // then
        assertThat(currentSpread).isEqualTo(expectedSpread);

    }

    private static BigDecimal calculateSpreadPercentageValue(Market market) {
        if (market.bidPrice() == null || market.askPrice() == null) {
            return null;
        }

        return ((market.askPrice().subtract(market.bidPrice()))
                .divide(new BigDecimal("0.5")
                        .multiply(market.askPrice().add(market.bidPrice())), 4, RoundingMode.HALF_UP))
                .multiply(new BigDecimal("100"));
    }
}