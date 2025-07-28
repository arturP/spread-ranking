package io.artur.interview.kanga.spread_ranking.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Builder
@Getter
public class ErrorApiResponse {

    private final Instant timestamp;
    private final Integer status;
    private final String errorType;
    private final String errorMessage;
}
