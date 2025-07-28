package io.artur.interview.kanga.spread_ranking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class SpreadRankingResult {

    private final Instant timestamp;
    private final SpreadRankingData ranking;
}