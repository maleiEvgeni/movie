package com.dept.movie.application;

import com.dept.movie.api.dto.MovieDetailsResponse;
import com.dept.movie.api.dto.MovieSearchResponse;
import com.dept.movie.api.dto.MovieSummaryResponse;
import com.dept.movie.api.dto.TrailerResponse;
import com.dept.movie.domain.MovieType;
import com.dept.movie.integration.tmdb.TmdbClient;
import com.dept.movie.integration.tmdb.TmdbMapper;
import com.dept.movie.integration.tmdb.dto.TmdbMovieDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieSearchService {

    private static final int MAX_TRAILER_ENRICHED_RESULTS = 5;

    private final TmdbClient tmdbClient;
    private final TmdbMapper tmdbMapper;
    private final TrailerService trailerService;

    public MovieSearchResponse search(String query, int page, String language) {
        var tmdbResponse = tmdbClient.search(query, page, language);

        List<TmdbMovieDto> rawResults = tmdbResponse.results() == null
                ? List.of()
                : tmdbResponse.results().stream()
                .filter(this::supportedMediaType)
                .toList();

        List<MovieSummaryResponse> results = rawResults.stream()
                .map(movie -> {
                    MovieType type = resolveType(movie.mediaType());

                    TrailerResponse trailer = null;
                    int index = rawResults.indexOf(movie);

                    if (index < MAX_TRAILER_ENRICHED_RESULTS) {
                        trailer = trailerService.findTrailer(
                                movie.id(),
                                type,
                                movie.displayTitle(),
                                parseDate(movie.displayDate()),
                                language
                        );
                    }

                    return tmdbMapper.toSummary(movie, trailer);
                })
                .toList();

        return new MovieSearchResponse(
                query,
                tmdbResponse.page(),
                tmdbResponse.totalPages(),
                tmdbResponse.totalResults(),
                results
        );
    }

    public MovieDetailsResponse details(Long tmdbId, MovieType type, String language) {
        var details = tmdbClient.details(tmdbId, type, language);

        TrailerResponse trailer = trailerService.findTrailer(
                tmdbId,
                type,
                details.displayTitle(),
                parseDate(details.displayDate()),
                language
        );

        return tmdbMapper.toDetails(details, type, trailer);
    }

    private boolean supportedMediaType(TmdbMovieDto movie) {
        if (movie == null || movie.id() == null) {
            return false;
        }

        return "movie".equalsIgnoreCase(movie.mediaType())
                || "tv".equalsIgnoreCase(movie.mediaType())
                || movie.mediaType() == null;
    }

    private MovieType resolveType(String mediaType) {
        if ("tv".equalsIgnoreCase(mediaType)) {
            return MovieType.TV;
        }

        return MovieType.MOVIE;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDate.parse(value);
    }
}