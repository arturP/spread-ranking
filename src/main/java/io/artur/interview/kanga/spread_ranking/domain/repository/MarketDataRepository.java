package io.artur.interview.kanga.spread_ranking.domain.repository;

import io.artur.interview.kanga.spread_ranking.domain.model.Market;

import java.util.List;

public interface MarketDataRepository {

    void saveAll(List<Market> markets);
}
