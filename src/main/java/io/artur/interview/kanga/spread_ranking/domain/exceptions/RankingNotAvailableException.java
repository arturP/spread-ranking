package io.artur.interview.kanga.spread_ranking.domain.exceptions;

public class RankingNotAvailableException extends RuntimeException {
    public RankingNotAvailableException(String message) {
        super(message);
    }
}
