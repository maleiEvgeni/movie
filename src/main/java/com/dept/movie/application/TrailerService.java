package com.dept.movie.application;

import com.dept.movie.api.dto.TrailerResponse;
import com.dept.movie.cache.CacheNames;
import com.dept.movie.cache.TwoLevelCacheService;
import com.dept.movie.domain.MovieType;
import com.dept.movie.domain.TrailerCandidate;
import com.dept.movie.domain.TrailerProvider;
import com.dept.movie.integration.tmdb.TmdbClient;
import com.dept.movie.integration.youtube.YouTubeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrailerService {

    private final TmdbClient tmdbClient;
    private final YouTubeClient youtubeClient;
    private final TwoLevelCacheService cacheService;

    public TrailerResponse findTrailer(Long tmdbId, MovieType type, String title, LocalDate releaseDate, String language) {
        String key = "best:v1:%s:%s:%d".formatted(type.name().toLowerCase(), normalize(language), tmdbId);

        return cacheService.get(CacheNames.TRAILER, key, TrailerResponse.class)
                .orElseGet(() -> {
                    TrailerResponse trailer = resolveTrailer(tmdbId, type, title, releaseDate, language).orElse(null);
                    cacheService.put(CacheNames.TRAILER, key, trailer);
                    return trailer;
                });
    }

    private Optional<TrailerResponse> resolveTrailer(
            Long tmdbId,
            MovieType type,
            String title,
            LocalDate releaseDate,
            String language
    ) {
        Optional<TrailerResponse> tmdbTrailer = findFromTmdb(tmdbId, type, language);
        if (tmdbTrailer.isPresent()) {
            return tmdbTrailer;
        }

        try {
            return youtubeClient.searchTrailer(title, releaseDate)
                    .map(this::toResponse);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Optional<TrailerResponse> findFromTmdb(Long tmdbId, MovieType type, String language) {
        try {
            var videos = tmdbClient.videos(tmdbId, type, language);

            if (videos == null || videos.results() == null || videos.results().isEmpty()) {
                return Optional.empty();
            }

            return videos.results().stream()
                    .filter(video -> video != null && video.isYouTubeTrailer())
                    .sorted(Comparator
                            .comparing((com.dept.movie.integration.tmdb.dto.TmdbVideoDto video) -> !Boolean.TRUE.equals(video.official()))
                            .thenComparing(video -> video.name() == null ? "" : video.name()))
                    .findFirst()
                    .map(video -> new TrailerResponse(
                            "youtube",
                            "https://www.youtube.com/watch?v=" + video.key(),
                            "tmdb-videos",
                            Boolean.TRUE.equals(video.official())
                    ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private TrailerResponse toResponse(TrailerCandidate candidate) {
        return new TrailerResponse(
                providerName(candidate.provider()),
                candidate.url(),
                candidate.source(),
                candidate.official()
        );
    }

    private String providerName(TrailerProvider provider) {
        return provider == null ? "unknown" : provider.name().toLowerCase();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase().replaceAll("\\s+", "-");
    }
}