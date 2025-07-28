package io.artur.interview.kanga.spread_ranking.infrastructure.external;


import io.artur.interview.kanga.spread_ranking.domain.model.MarketPair;
import io.artur.interview.kanga.spread_ranking.domain.model.OrderBook;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.dto.KangaMarketPairResponse;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.dto.KangaOrderBookResponse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

class KangaApiMapper {

    public static MarketPair toDomainMarketPair(KangaMarketPairResponse response) {
        if (response == null || response.getTickerId() == null) {
            return null;
        }
        return new MarketPair(
                response.getTickerId(),
                response.getBaseCurrency(),
                response.getTargetCurrency());
    }

    public static OrderBook toDomainOrderBook(KangaOrderBookResponse response, Clock clock) {
        if (response == null) {
            return OrderBook.empty("", clock);
        }
        Optional<BigDecimal> betsBid = response.getBestBidPrice();
        Optional<BigDecimal> bestAsk = response.getBestAskPrice();

        return new OrderBook(
                response.getTickerId(),
                betsBid.orElse(null),
                bestAsk.orElse(null),
                Instant.ofEpochMilli(response.getTimestamp() != null ? response.getTimestamp() : Instant.now(clock).toEpochMilli()));
    }
}
