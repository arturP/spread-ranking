package io.artur.interview.kanga.spread_ranking.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpreadDto {

    private final String market;
    private final String spread;

    public SpreadDto(String market, String spread) {
        this.market = market;
        this.spread = spread;
    }

    public String getMarket() {
        return market;
    }

    public String getSpread() {
        return spread;
    }
}
