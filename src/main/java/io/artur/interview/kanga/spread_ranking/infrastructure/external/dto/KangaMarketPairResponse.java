package io.artur.interview.kanga.spread_ranking.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class KangaMarketPairResponse {

    @JsonProperty("ticker_id")
    private String tickerId;
    @JsonProperty("base")
    private String baseCurrency;
    @JsonProperty("target")
    private String targetCurrency;
}
