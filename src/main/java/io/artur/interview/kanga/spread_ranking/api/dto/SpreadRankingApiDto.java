package io.artur.interview.kanga.spread_ranking.api.dto;

import io.artur.interview.kanga.spread_ranking.domain.model.Spread;
import io.artur.interview.kanga.spread_ranking.domain.model.SpreadRanking;

import java.util.Comparator;
import java.util.List;

public record SpreadRankingApiDto(List<SpreadApiDto> group1, List<SpreadApiDto> group2, List<SpreadApiDto> group3) {

    public SpreadRankingApiDto(List<SpreadApiDto> group1, List<SpreadApiDto> group2, List<SpreadApiDto> group3) {
        this.group1 = sortAlphabetically(group1);
        this.group2 = sortAlphabetically(group2);
        this.group3 = sortAlphabetically(group3);
    }

    public static SpreadRankingApiDto fromSpreadRanking(SpreadRanking domainRanking) {
        return new SpreadRankingApiDto(
                transformToApiDto(domainRanking.getLowSpreadMarkets()),
                transformToApiDto(domainRanking.getHighSpreadMarkets()),
                transformToUnavailableApiDto(domainRanking.getUnavailableMarkets())
        );
    }

    private static List<SpreadApiDto> sortAlphabetically(List<SpreadApiDto> spreads) {
        return spreads.stream()
                .sorted(Comparator.comparing(SpreadApiDto::market, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static List<SpreadApiDto> transformToApiDto(List<Spread> spreads) {
        return spreads.stream()
                .map(SpreadApiDto::fromDomainSpread)
                .toList();
    }

    private static List<SpreadApiDto> transformToUnavailableApiDto(List<Spread> spreads) {
        return spreads.stream()
                .map(spread -> SpreadApiDto.unknown(spread.marketId()))
                .toList();
    }
}