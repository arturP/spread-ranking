package io.artur.interview.kanga.spread_ranking.domain.model;

import lombok.Getter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Getter
public class SpreadRanking {

    private final List<Spread> lowSpreadMarkets;
    private final List<Spread> highSpreadMarkets;
    private final List<Spread> unavailableMarkets;
    private final Instant calculatedAt;
    private final int totalMarketsCount;

    private SpreadRanking(Builder builder) {
        this.lowSpreadMarkets = List.copyOf(builder.lowSpreadMarkets);
        this.highSpreadMarkets = List.copyOf(builder.highSpreadMarkets);
        this.unavailableMarkets = List.copyOf(builder.unavailableMarkets);
        this.calculatedAt = builder.calculatedAt;
        this.totalMarketsCount = lowSpreadMarkets.size() +
                highSpreadMarkets.size() +
                unavailableMarkets.size();
    }


    public static SpreadRanking empty(Clock clock) {
        return builder()
                .lowSpreadMarkets(List.of())
                .highSpreadMarkets(List.of())
                .unavailableMarkets(List.of())
                .calculatedAt(Instant.now(clock))
                .build();
    }

    public boolean isOlderThan(Duration age, Clock clock) {
        return calculatedAt.plus(age).isBefore(Instant.now(clock));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Spread> lowSpreadMarkets = new ArrayList<>();
        private List<Spread> highSpreadMarkets = new ArrayList<>();
        private List<Spread> unavailableMarkets = new ArrayList<>();
        private Instant calculatedAt;

        public Builder lowSpreadMarkets(List<Spread> spreads) {
            this.lowSpreadMarkets = spreads != null ? spreads : List.of();
            return this;
        }

        public Builder highSpreadMarkets(List<Spread> spreads) {
            this.highSpreadMarkets = spreads != null ? spreads : List.of();
            return this;
        }

        public Builder unavailableMarkets(List<Spread> spreads) {
            this.unavailableMarkets = spreads != null ? spreads : List.of();
            return this;
        }

        public Builder calculatedAt(Instant timestamp) {
            this.calculatedAt = timestamp;
            return this;
        }

        public SpreadRanking build() {
            return new SpreadRanking(this);
        }
    }
}
