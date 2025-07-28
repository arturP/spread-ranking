package io.artur.interview.kanga.spread_ranking.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KangaMarketPairResponse {

    private String tickerId;
    private String baseCurrency;
    private String targetCurrency;
}
