package io.artur.interview.kanga.spread_ranking.infrastructure.external.config;

import io.artur.interview.kanga.spread_ranking.domain.ExchangeApiClient;
import io.artur.interview.kanga.spread_ranking.infrastructure.external.KangaApiClientOptimized;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;

@Configuration
public class KangaClientConfiguration {

    /*@Bean
    ExchangeApiClient kangaApiClient(RestTemplate restTemplate, Clock clock) {
        return new KangaApiClient(restTemplate, clock);
    }*/

    @Bean
    ExchangeApiClient kangaApiClientOptimized(WebClient.Builder webClientBuilder, Clock clock) {
        return new KangaApiClientOptimized(webClientBuilder, clock);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
