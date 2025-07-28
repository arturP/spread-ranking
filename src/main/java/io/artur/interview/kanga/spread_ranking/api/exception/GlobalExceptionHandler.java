package io.artur.interview.kanga.spread_ranking.api.exception;

import io.artur.interview.kanga.spread_ranking.api.dto.ErrorApiResponse;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.RankingNotAvailableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(RankingNotAvailableException.class)
    ResponseEntity<ErrorApiResponse> handleRankingNotAvailable(
            RankingNotAvailableException exception,
            HttpServletRequest request) {
        log.info("Ranking not available: {}", exception.getMessage());

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.NOT_FOUND.value())
                .errorType("Ranking is not available now")
                .errorMessage(exception.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
