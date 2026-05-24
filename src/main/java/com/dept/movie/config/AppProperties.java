package com.dept.movie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "movie")
public record AppProperties(
        Tmdb tmdb,
        Youtube youtube,
        Cache cache,
        Resilience resilience
) {
    public record Tmdb(
            String baseUrl,
            String imageBaseUrl,
            String apiKey,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }

    public record Youtube(
            String baseUrl,
            String apiKey,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }

    public record Cache(
            Duration searchL1Ttl,
            Duration searchL2Ttl,
            Duration detailsL1Ttl,
            Duration detailsL2Ttl,
            Duration trailerL1Ttl,
            Duration trailerL2Ttl
    ) {
    }

    public record Resilience(
            Provider tmdb,
            Provider youtube
    ) {
    }

    public record Provider(
            Duration timeout,
            Duration rateLimitPeriod,
            int rateLimitPermits,
            int bulkheadMaxConcurrent,
            int circuitBreakerFailureRateThreshold
    ) {
    }
}