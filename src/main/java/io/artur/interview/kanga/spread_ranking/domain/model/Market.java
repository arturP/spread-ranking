package io.artur.interview.kanga.spread_ranking.domain.model;

import java.math.BigDecimal;

public record Market(String tickerId, BigDecimal bidPrice, BigDecimal askPrice) {}
