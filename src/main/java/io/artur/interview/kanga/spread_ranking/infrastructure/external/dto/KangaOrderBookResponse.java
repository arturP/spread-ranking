package io.artur.interview.kanga.spread_ranking.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KangaOrderBookResponse {

    @JsonProperty("ticker_id")
    private String tickerId;
    @JsonProperty("bids")
    private List<List<String>> bids = new ArrayList<>();
    @JsonProperty("asks")
    private List<List<String>> asks = new ArrayList<>();
    @JsonProperty("timestamp")
    private Long timestamp;

    public Optional<BigDecimal> getBestBidPrice() {
        return bids.stream()
                .findFirst()  // First bid is the highest price
                .map(bid -> new BigDecimal(bid.getFirst()));
    }

    public Optional<BigDecimal> getBestAskPrice() {
        return asks.stream()
                .findFirst()  // First ask is the lowest price
                .map(ask -> new BigDecimal(ask.getFirst()));
    }
}
