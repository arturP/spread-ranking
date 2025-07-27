package io.artur.interview.kanga.spread_ranking.domain.exceptions;

public class SpreadCalculationException extends RuntimeException {
    public SpreadCalculationException(String message, Exception ex) {
        super(message, ex);
    }
}
