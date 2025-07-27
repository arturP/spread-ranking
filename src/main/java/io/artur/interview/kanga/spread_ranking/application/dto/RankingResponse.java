package io.artur.interview.kanga.spread_ranking.application.dto;

import java.time.Instant;

public class RankingResponse {

    private final Instant timestamp;
    private final RankingDto ranking;

    public RankingResponse(Instant timestamp, RankingDto ranking) {
        this.timestamp = timestamp;
        this.ranking = ranking;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public RankingDto getRanking() {
        return ranking;
    }
}
