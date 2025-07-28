package io.artur.interview.kanga.spread_ranking.api.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CalculationApiResponse(String message, Instant timestamp, Long calculationDurationMs, String status, String error) {
}
