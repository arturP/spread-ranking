package io.artur.interview.kanga.spread_ranking.application.dto;

import java.util.List;

public class RankingDto {

    private final List<SpreadDto> group1;
    private final List<SpreadDto> group2;
    private final List<SpreadDto> group3;

    public RankingDto(List<SpreadDto> group1, List<SpreadDto> group2, List<SpreadDto> group3) {
        this.group1 = group1;
        this.group2 = group2;
        this.group3 = group3;
    }

    public List<SpreadDto> getGroup1() {
        return group1;
    }

    public List<SpreadDto> getGroup2() {
        return group2;
    }

    public List<SpreadDto> getGroup3() {
        return group3;
    }
}
