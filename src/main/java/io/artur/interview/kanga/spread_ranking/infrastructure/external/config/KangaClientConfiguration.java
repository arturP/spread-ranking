package io.artur.interview.kanga.spread_ranking.infrastructure.external.config;

import io.artur.interview.kanga.spread_ranking.domain.ExchangeApiClient;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.KangaApiClientOptimized;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class KangaClientConfiguration {

    @Bean
    public ConnectionProvider kangaConnectionProvider(KangaApiProperties properties) {
        var poolConfig = properties.getConnectionPool();
        return ConnectionProvider.builder("kanga-pool")
                .maxConnections(poolConfig.getMaxConnections())
                .maxIdleTime(Duration.ofSeconds(poolConfig.getMaxIdleTime()))
                .maxLifeTime(Duration.ofSeconds(poolConfig.getMaxLifeTime()))
                .pendingAcquireTimeout(poolConfig.getPendingAcquireTimeout())
                .evictInBackground(poolConfig.getEvictInBackground())
                .build();
    }

    @Bean
    public HttpClient kangaHttpClient(ConnectionProvider connectionProvider, KangaApiProperties properties) {
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getOperationTimeout().toMillis())
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(properties.getOperationTimeout().toSeconds(), TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(properties.getOperationTimeout().toSeconds(), TimeUnit.SECONDS)));
    }

    @Bean
    public WebClient kangaWebClient(HttpClient httpClient, KangaApiProperties properties) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(properties.getMaxInMemorySize()))
                .build();
    }

    @Bean
    public CircuitBreaker kangaCircuitBreaker(KangaApiProperties properties) {
        var cbConfig = properties.getCircuitBreaker();
        var config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.getFailureRateThreshold())
                .slowCallRateThreshold(cbConfig.getSlowCallRateThreshold())
                .slowCallDurationThreshold(cbConfig.getSlowCallDurationThreshold())
                .slidingWindowSize(cbConfig.getSlidingWindowSize())
                .minimumNumberOfCalls(cbConfig.getMinimumNumberOfCalls())
                .waitDurationInOpenState(cbConfig.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(cbConfig.getPermittedNumberOfCallsInHalfOpenState())
                .build();
        return CircuitBreaker.of("kanga-api", config);
    }

    @Bean
    ExchangeApiClient kangaApiClientOptimized(WebClient webClient, CircuitBreaker circuitBreaker, 
                                             Clock clock, KangaApiProperties properties) {
        return new KangaApiClientOptimized(webClient, circuitBreaker, clock, properties);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
