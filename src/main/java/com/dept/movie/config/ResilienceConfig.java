package com.dept.movie.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    CircuitBreaker tmdbCircuitBreaker(AppProperties properties) {
        return providerCircuitBreaker("tmdb", properties.resilience().tmdb());
    }

    @Bean
    CircuitBreaker youtubeCircuitBreaker(AppProperties properties) {
        return providerCircuitBreaker("youtube", properties.resilience().youtube());
    }

    @Bean
    RateLimiter tmdbRateLimiter(AppProperties properties) {
        return providerRateLimiter("tmdb", properties.resilience().tmdb());
    }

    @Bean
    RateLimiter youtubeRateLimiter(AppProperties properties) {
        return providerRateLimiter("youtube", properties.resilience().youtube());
    }

    @Bean
    Bulkhead tmdbBulkhead(AppProperties properties) {
        return providerBulkhead("tmdb", properties.resilience().tmdb());
    }

    @Bean
    Bulkhead youtubeBulkhead(AppProperties properties) {
        return providerBulkhead("youtube", properties.resilience().youtube());
    }

    @Bean
    Retry externalApiRetry() {
        var config = io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(300))
                .retryExceptions(
                        java.net.SocketTimeoutException.class,
                        java.io.IOException.class,
                        org.springframework.web.client.ResourceAccessException.class
                )
                .build();

        return Retry.of("external-api", config);
    }

    private CircuitBreaker providerCircuitBreaker(String name, AppProperties.Provider provider) {
        var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(provider.circuitBreakerFailureRateThreshold())
                .slidingWindowSize(30)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordException(this::isCircuitBreakerException)
                .build();

        return CircuitBreaker.of(name, config);
    }

    private RateLimiter providerRateLimiter(String name, AppProperties.Provider provider) {
        var config = io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitRefreshPeriod(provider.rateLimitPeriod())
                .limitForPeriod(provider.rateLimitPermits())
                .timeoutDuration(Duration.ZERO)
                .build();

        return RateLimiter.of(name, config);
    }

    private Bulkhead providerBulkhead(String name, AppProperties.Provider provider) {
        var config = io.github.resilience4j.bulkhead.BulkheadConfig.custom()
                .maxConcurrentCalls(provider.bulkheadMaxConcurrent())
                .maxWaitDuration(Duration.ZERO)
                .build();

        return Bulkhead.of(name, config);
    }

    private boolean isCircuitBreakerException(Throwable throwable) {
        if (throwable instanceof org.springframework.web.client.HttpClientErrorException clientError) {
            int status = clientError.getStatusCode().value();
            return status == 408 || status == 429;
        }

        if (throwable instanceof org.springframework.web.client.HttpServerErrorException) {
            return true;
        }

        return throwable instanceof org.springframework.web.client.ResourceAccessException;
    }
}