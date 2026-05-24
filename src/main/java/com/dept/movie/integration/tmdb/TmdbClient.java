package com.dept.movie.integration.tmdb;

import com.dept.movie.cache.CacheNames;
import com.dept.movie.cache.TwoLevelCacheService;
import com.dept.movie.domain.MovieType;
import com.dept.movie.integration.tmdb.dto.TmdbMovieDetailsDto;
import com.dept.movie.integration.tmdb.dto.TmdbSearchResponse;
import com.dept.movie.integration.tmdb.dto.TmdbVideosResponse;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

@Component
public class TmdbClient {

    private final RestClient tmdbRestClient;
    private final TwoLevelCacheService cacheService;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    private final Bulkhead bulkhead;
    private final Retry retry;

    public TmdbClient(
            @Qualifier("tmdbRestClient") RestClient tmdbRestClient,
            TwoLevelCacheService cacheService,
            @Qualifier("tmdbCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("tmdbRateLimiter") RateLimiter rateLimiter,
            @Qualifier("tmdbBulkhead") Bulkhead bulkhead,
            Retry externalApiRetry
    ) {
        this.tmdbRestClient = tmdbRestClient;
        this.cacheService = cacheService;
        this.circuitBreaker = circuitBreaker;
        this.rateLimiter = rateLimiter;
        this.bulkhead = bulkhead;
        this.retry = externalApiRetry;
    }

    public TmdbSearchResponse search(String query, int page, String language) {
        String key = "v1:%s:%d:%s".formatted(normalize(language), page, normalize(query));

        return cacheService.getOrLoad(
                CacheNames.SEARCH,
                key,
                TmdbSearchResponse.class,
                () -> execute(() -> tmdbRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search/multi")
                                .queryParam("query", query)
                                .queryParam("page", page)
                                .queryParam("language", language)
                                .queryParam("include_adult", false)
                                .build())
                        .retrieve()
                        .body(TmdbSearchResponse.class))
        );
    }

    public TmdbMovieDetailsDto details(Long tmdbId, MovieType type, String language) {
        String key = "v1:%s:%s:%d".formatted(type.name().toLowerCase(), normalize(language), tmdbId);

        return cacheService.getOrLoad(
                CacheNames.DETAILS,
                key,
                TmdbMovieDetailsDto.class,
                () -> execute(() -> tmdbRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(type == MovieType.TV ? "/tv/{id}" : "/movie/{id}")
                                .queryParam("language", language)
                                .build(tmdbId))
                        .retrieve()
                        .body(TmdbMovieDetailsDto.class))
        );
    }

    public TmdbVideosResponse videos(Long tmdbId, MovieType type, String language) {
        String key = "videos:v1:%s:%s:%d".formatted(type.name().toLowerCase(), normalize(language), tmdbId);

        return cacheService.getOrLoad(
                CacheNames.TRAILER,
                key,
                TmdbVideosResponse.class,
                () -> execute(() -> tmdbRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(type == MovieType.TV ? "/tv/{id}/videos" : "/movie/{id}/videos")
                                .queryParam("language", language)
                                .build(tmdbId))
                        .retrieve()
                        .body(TmdbVideosResponse.class))
        );
    }

    private <T> T execute(Supplier<T> supplier) {
        Supplier<T> decorated = supplier;

        decorated = Bulkhead.decorateSupplier(bulkhead, decorated);
        decorated = RateLimiter.decorateSupplier(rateLimiter, decorated);
        decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
        decorated = Retry.decorateSupplier(retry, decorated);

        return decorated.get();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase().replaceAll("\\s+", "-");
    }
}