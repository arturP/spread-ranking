package io.artur.interview.kanga.spread_ranking.domain.repository;

import io.artur.interview.kanga.spread_ranking.domain.model.SpreadRanking;

import java.util.Optional;

public interface SpreadRankingRepository {

    void storeSpreadRanking(SpreadRanking spreadRanking);
    Optional<SpreadRanking> getCurrentSpreadRanking();
    boolean hasValidSpreadRanking();
    void clear();
    boolean isRankingExpired();
}
