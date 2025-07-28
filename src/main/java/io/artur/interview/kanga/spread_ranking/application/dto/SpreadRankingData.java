package io.artur.interview.kanga.spread_ranking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SpreadRankingData {

    private final List<SpreadDto> group1;
    private final List<SpreadDto> group2;
    private final List<SpreadDto> group3;
}