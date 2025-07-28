package io.artur.interview.kanga.spread_ranking.infrastructure.external.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setRequestFactory(createRequestFactory());

        restTemplate.setErrorHandler(new ErrorHandler());

        restTemplate.setInterceptors(List.of(new HeadersRequestInterceptor()));

        return restTemplate;
    }

    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Connection timeout - time to establish connection
        factory.setConnectTimeout(5000); // 5 seconds

        // Read timeout - time to read response
        factory.setReadTimeout(10000); // 10 seconds

        return factory;
    }

    @Component
    public static class ErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            // We'll handle errors in the client code, not here
            return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            // No-op - we handle errors in client
        }
    }

    @Component
    public static class HeadersRequestInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(
                HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws IOException {

            // Add standard headers
            request.getHeaders().add(HttpHeaders.USER_AGENT, "SpreadRankingService/1.0");
            request.getHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            return execution.execute(request, body);
        }
    }
}
