package io.artur.interview.kanga.spread_ranking.domain;


import io.artur.interview.kanga.spread_ranking.domain.model.Market;
import io.artur.interview.kanga.spread_ranking.domain.model.Spread;
import io.artur.interview.kanga.spread_ranking.domain.model.SpreadCategory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static io.artur.interview.kanga.spread_ranking.domain.model.SpreadCategory.HIGH_SPREAD;
import static io.artur.interview.kanga.spread_ranking.domain.model.SpreadCategory.LOW_SPREAD;


/**
 * Service for calculation of spread.
 * If there is a missing ask or bid price, the spread is UNKNOWN.
 * <p>
 * Calculation of a percentage spread is:
 * SpreadPct = (askPrice - bidPrice)/(0.5 * (askPrice + bidPrice)) * 100
 * <p>
 * If spread percentage value is greater than 2, it is categorized as HIGH_SPREAD.
 * Otherwise, it is categorized as LOW_SPREAD.
 */
@Service
public class SpreadCalculationService {

    private static final int DIVIDE_SCALE = 4;

    public Spread calculateSpread(final Market market) {
        if (market.askPrice() == null || market.bidPrice() == null) {
            return Spread.unknown(market.tickerId());
        }

        final BigDecimal spreadPctValue = (market.askPrice().subtract(market.bidPrice()))
                .divide(new BigDecimal("0.5")
                        .multiply(market.askPrice().add(market.bidPrice())), DIVIDE_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return new Spread(market.tickerId(), spreadPctValue, categorizeSpread(spreadPctValue));
    }

    private SpreadCategory categorizeSpread(final BigDecimal spreadPctValue) {
        if (spreadPctValue.compareTo(new BigDecimal("2.0")) > 0) {
            return HIGH_SPREAD;
        }
        return LOW_SPREAD;
    }
}
