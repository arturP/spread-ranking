package io.artur.interview.kanga.spread_ranking.domain.model;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@RequiredArgsConstructor
@Getter
public class OrderBook {

    private final String marketId;
    private final BigDecimal bestBidPrice;
    private final BigDecimal bestAskPrice;
    private final Instant timestamp;

    public static OrderBook empty(String marketId, Clock clock) {
        return new OrderBook(marketId, null, null, Instant.now(clock));
    }

    public boolean isEmpty() {
        return bestAskPrice == null || bestBidPrice == null;
    }


}
