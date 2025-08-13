package io.artur.interview.kanga.spread_ranking.api.exception;

import io.artur.interview.kanga.spread_ranking.api.dto.ErrorApiResponse;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.ExchangeApiException;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.RankingNotAvailableException;
import io.artur.interview.kanga.spread_ranking.domain.exceptions.SpreadCalculationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

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

    @ExceptionHandler(SpreadCalculationException.class)
    ResponseEntity<ErrorApiResponse> handleSpreadCalculationException(
            SpreadCalculationException exception,
            HttpServletRequest request) {
        log.error("Spread calculation failed: {}", exception.getMessage(), exception);

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorType("Spread calculation failed")
                .errorMessage("Failed to calculate spread ranking. Please try again later.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ExchangeApiException.class)
    ResponseEntity<ErrorApiResponse> handleExchangeApiException(
            ExchangeApiException exception,
            HttpServletRequest request) {
        log.error("Exchange API error: {}", exception.getMessage(), exception);

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.BAD_GATEWAY.value())
                .errorType("External service unavailable")
                .errorMessage("Exchange service is temporarily unavailable. Please try again later.")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorApiResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        log.warn("Invalid request parameter: {}", exception.getMessage());

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.BAD_REQUEST.value())
                .errorType("Invalid request parameter")
                .errorMessage(exception.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorApiResponse> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request) {
        log.warn("Access denied: {}", exception.getMessage());

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.FORBIDDEN.value())
                .errorType("Access denied")
                .errorMessage("Insufficient permissions to access this resource")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorApiResponse> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        String violations = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("Request validation failed: {}", violations);

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.BAD_REQUEST.value())
                .errorType("Validation failed")
                .errorMessage("Invalid request parameters: " + violations)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApiResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
                
        log.warn("Request body validation failed: {}", errors);

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.BAD_REQUEST.value())
                .errorType("Validation failed")
                .errorMessage("Invalid request body: " + errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorApiResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        log.warn("Parameter type mismatch: {} should be of type {}", 
                exception.getName(), exception.getRequiredType().getSimpleName());

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.BAD_REQUEST.value())
                .errorType("Parameter type mismatch")
                .errorMessage(String.format("Parameter '%s' should be of type %s", 
                        exception.getName(), exception.getRequiredType().getSimpleName()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorApiResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", exception.getMessage(), exception);

        ErrorApiResponse response = ErrorApiResponse.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorType("Internal server error")
                .errorMessage("An unexpected error occurred. Please try again later.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
