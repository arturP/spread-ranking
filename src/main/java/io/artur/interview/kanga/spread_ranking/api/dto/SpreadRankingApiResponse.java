package io.artur.interview.kanga.spread_ranking.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.artur.interview.kanga.spread_ranking.domain.model.SpreadRanking;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpreadRankingApiResponse(Instant timestamp, SpreadRankingApiDto ranking) {

    public static SpreadRankingApiResponse create(SpreadRanking spreadRanking, Clock clock) {
        return new SpreadRankingApiResponse(
                Instant.now(clock),
                SpreadRankingApiDto.fromSpreadRanking(spreadRanking)
        );
    }
}