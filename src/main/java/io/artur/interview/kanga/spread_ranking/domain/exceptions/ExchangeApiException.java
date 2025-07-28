package io.artur.interview.kanga.spread_ranking.domain.exceptions;

public class ExchangeApiException extends RuntimeException {
    public ExchangeApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExchangeApiException(String message) {
        super(message);
    }
}
