package io.artur.interview.kanga.spread_ranking.api;

import io.artur.interview.kanga.spread_ranking.api.dto.CalculationApiResponse;
import io.artur.interview.kanga.spread_ranking.application.SpreadRankingService;
import io.artur.interview.kanga.spread_ranking.api.dto.SpreadRankingApiResponse;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.RankingNotAvailableException;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.SpreadCalculationException;
import io.artur.interview.kanga.spread_ranking.domain.model.SpreadRanking;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/spread")
@Validated
@RequiredArgsConstructor
class SpreadController {

    private final SpreadRankingService spreadRankingService;
    private final Clock clock;

    @GetMapping("/ranking")
    @PreAuthorize("hasRole('API_USER')")
    public ResponseEntity<SpreadRankingApiResponse> getRanking() {
        log.info("Received request for current spread ranking");
        try {
            SpreadRanking ranking;
            
            if (spreadRankingService.isRankingCurrent()) {
                ranking = spreadRankingService.getCurrentRanking();
                log.debug("Using cached ranking");
            } else {
                ranking = spreadRankingService.calculateSpreadRanking();
                spreadRankingService.storeSpreadRanking(ranking);
                log.info("Calculated fresh ranking");
            }
            
            SpreadRankingApiResponse response = SpreadRankingApiResponse.create(ranking, clock);
            log.info("Successfully retrieved ranking with {} total markets", ranking.getTotalMarketsCount());
            return ResponseEntity.ok(response);
            
        } catch (RankingNotAvailableException ex) {
            log.warn("Ranking not available: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SpreadCalculationException ex) {
            log.error("Failed to calculate ranking: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasRole('API_USER')")
    public ResponseEntity<CalculationApiResponse> calculateRanking(
            HttpServletRequest request) {

        log.info("Received calculation request");

        try {
            long startTime = System.currentTimeMillis();

            SpreadRanking ranking = spreadRankingService.calculateSpreadRanking();

            spreadRankingService.storeSpreadRanking(ranking);

            long duration = System.currentTimeMillis() - startTime;

            CalculationApiResponse response = CalculationApiResponse.builder()
                    .message("Ranking calculated successfully")
                    .timestamp(Instant.now(clock))
                    .calculationDurationMs(duration)
                    .status("SUCCESS")
                    .build();

            log.info("Successfully calculated ranking in {}ms", duration);

            return ResponseEntity.ok().body(response);

        } catch (SpreadCalculationException ex) {
            log.error("Failed to calculate ranking", ex);

            CalculationApiResponse response = CalculationApiResponse.builder()
                    .message("Calculation failed: " + ex.getMessage())
                    .timestamp(Instant.now())
                    .status("ERROR")
                    .error(ex.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error during calculation", ex);

            CalculationApiResponse response = CalculationApiResponse.builder()
                    .message("Internal server error occurred")
                    .timestamp(Instant.now())
                    .status("ERROR")
                    .error("An unexpected error occurred. Please try again later.")
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
