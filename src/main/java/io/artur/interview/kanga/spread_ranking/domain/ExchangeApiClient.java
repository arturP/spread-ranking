package io.artur.interview.kanga.spread_ranking.domain;

import io.artur.interview.kanga.spread_ranking.domain.model.MarketPair;
import io.artur.interview.kanga.spread_ranking.domain.model.OrderBook;

import java.util.List;
import java.util.Map;

public interface ExchangeApiClient {

    List<MarketPair> getMarketPairs();
    Map<String, OrderBook> getOrderBooks(List<String> marketIds);
}
