package com.dept.movie.integration.youtube;

import com.dept.movie.config.AppProperties;
import com.dept.movie.domain.TrailerCandidate;
import com.dept.movie.domain.TrailerProvider;
import com.dept.movie.integration.tmdb.dto.youtube.YouTubeSearchResponse;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class YouTubeClient {

    private final RestClient youtubeRestClient;
    private final AppProperties properties;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    private final Bulkhead bulkhead;
    private final Retry retry;

    public YouTubeClient(
            @Qualifier("youtubeRestClient") RestClient youtubeRestClient,
            AppProperties properties,
            @Qualifier("youtubeCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("youtubeRateLimiter") RateLimiter rateLimiter,
            @Qualifier("youtubeBulkhead") Bulkhead bulkhead,
            Retry externalApiRetry
    ) {
        this.youtubeRestClient = youtubeRestClient;
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        this.rateLimiter = rateLimiter;
        this.bulkhead = bulkhead;
        this.retry = externalApiRetry;
    }

    public Optional<TrailerCandidate> searchTrailer(String title, LocalDate releaseDate) {
        if (properties.youtube().apiKey() == null || properties.youtube().apiKey().isBlank()) {
            return Optional.empty();
        }

        String year = releaseDate == null ? "" : String.valueOf(releaseDate.getYear());
        String query = "%s %s official trailer".formatted(title, year).trim();

        YouTubeSearchResponse response = execute(() -> youtubeRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("type", "video")
                        .queryParam("maxResults", 1)
                        .queryParam("q", query)
                        .queryParam("key", properties.youtube().apiKey())
                        .build())
                .retrieve()
                .body(YouTubeSearchResponse.class));

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return Optional.empty();
        }

        var item = response.items().getFirst();

        if (item.id() == null || item.id().videoId() == null || item.id().videoId().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new TrailerCandidate(
                TrailerProvider.YOUTUBE,
                "https://www.youtube.com/watch?v=" + item.id().videoId(),
                "youtube-search",
                false,
                item.snippet() == null ? null : item.snippet().title()
        ));
    }

    private <T> T execute(Supplier<T> supplier) {
        Supplier<T> decorated = supplier;

        decorated = Bulkhead.decorateSupplier(bulkhead, decorated);
        decorated = RateLimiter.decorateSupplier(rateLimiter, decorated);
        decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
        decorated = Retry.decorateSupplier(retry, decorated);

        return decorated.get();
    }
}