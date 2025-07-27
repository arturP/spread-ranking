package io.artur.interview.kanga.spread_ranking.domain;

import java.math.BigDecimal;

public record Market(String tickerId, BigDecimal bidPrice, BigDecimal askPrice) {}
