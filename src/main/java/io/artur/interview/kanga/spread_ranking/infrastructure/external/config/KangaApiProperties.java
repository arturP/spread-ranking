package io.artur.interview.kanga.spread_ranking.infrastructure.external.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "kanga.api")
public class KangaApiProperties {

    private String baseUrl = "https://public.kanga.exchange/api";
    private Duration operationTimeout = Duration.ofSeconds(10);
    private Duration pipelineTimeout = Duration.ofSeconds(30);
    private int retryCount = 3;
    private int maxInMemorySize = 1024 * 1024; // 1MB

    private ConnectionPool connectionPool = new ConnectionPool();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Data
    public static class ConnectionPool {
        private int maxConnections = 100;
        private int maxIdleTime = 20;
        private int maxLifeTime = 60;
        private Duration pendingAcquireTimeout = Duration.ofSeconds(45);
        private Duration evictInBackground = Duration.ofSeconds(120);
    }

    @Data
    public static class CircuitBreaker {
        private int failureRateThreshold = 50;
        private int slowCallRateThreshold = 50;
        private Duration slowCallDurationThreshold = Duration.ofSeconds(5);
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private int permittedNumberOfCallsInHalfOpenState = 3;
    }
}