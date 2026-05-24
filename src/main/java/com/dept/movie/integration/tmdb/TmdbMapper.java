package com.dept.movie.integration.tmdb;

import com.dept.movie.api.dto.MovieDetailsResponse;
import com.dept.movie.api.dto.MovieSummaryResponse;
import com.dept.movie.api.dto.TrailerResponse;
import com.dept.movie.config.AppProperties;
import com.dept.movie.domain.MovieType;
import com.dept.movie.integration.tmdb.dto.TmdbMovieDetailsDto;
import com.dept.movie.integration.tmdb.dto.TmdbMovieDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TmdbMapper {

    private final String imageBaseUrl;

    public TmdbMapper(AppProperties properties) {
        this.imageBaseUrl = properties.tmdb().imageBaseUrl();
    }

    public MovieSummaryResponse toSummary(TmdbMovieDto dto, TrailerResponse trailer) {
        MovieType type = resolveType(dto.mediaType());

        return new MovieSummaryResponse(
                externalId(type, dto.id()),
                dto.id(),
                dto.displayTitle(),
                dto.overview(),
                parseDate(dto.displayDate()),
                imageUrl("w500", dto.posterPath()),
                imageUrl("w780", dto.backdropPath()),
                dto.voteAverage(),
                type.name(),
                trailer
        );
    }

    public MovieDetailsResponse toDetails(TmdbMovieDetailsDto dto, MovieType type, TrailerResponse trailer) {
        List<String> genres = dto.genres() == null
                ? List.of()
                : dto.genres().stream()
                .map(TmdbMovieDetailsDto.TmdbGenreDto::name)
                .toList();

        return new MovieDetailsResponse(
                externalId(type, dto.id()),
                dto.id(),
                dto.displayTitle(),
                dto.overview(),
                parseDate(dto.displayDate()),
                dto.displayRuntime(),
                imageUrl("w500", dto.posterPath()),
                imageUrl("w780", dto.backdropPath()),
                dto.voteAverage(),
                genres,
                type.name(),
                trailer
        );
    }

    private MovieType resolveType(String mediaType) {
        if ("tv".equalsIgnoreCase(mediaType)) {
            return MovieType.TV;
        }

        return MovieType.MOVIE;
    }

    private String externalId(MovieType type, Long id) {
        return type.name().toLowerCase() + ":" + id;
    }

    private String imageUrl(String size, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        return imageBaseUrl + "/" + size + path;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDate.parse(value);
    }
}