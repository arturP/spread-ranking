package io.artur.interview.kanga.spread_ranking.domain;

import java.math.BigDecimal;

import static io.artur.interview.kanga.spread_ranking.domain.SpreadCategory.UNKNOWN;

public record Spread(String marketId, BigDecimal percentage, SpreadCategory category) {

    public static Spread unknown(String marketId) {
        return new Spread(marketId, null, UNKNOWN);
    }
}
