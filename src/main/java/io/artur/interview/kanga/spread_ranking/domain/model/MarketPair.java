package io.artur.interview.kanga.spread_ranking.domain.model;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class MarketPair {

    private final String tickerId;
    private final String baseCurrency;
    private final String targetCurrency;
}
