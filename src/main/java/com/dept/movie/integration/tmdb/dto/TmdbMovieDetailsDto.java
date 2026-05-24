package com.dept.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbMovieDetailsDto(
        Long id,
        String title,
        String name,
        String overview,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("first_air_date") String firstAirDate,
        Integer runtime,
        @JsonProperty("episode_run_time") List<Integer> episodeRunTime,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        @JsonProperty("vote_average") Double voteAverage,
        List<TmdbGenreDto> genres
) {
    public String displayTitle() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return name;
    }

    public String displayDate() {
        if (releaseDate != null && !releaseDate.isBlank()) {
            return releaseDate;
        }
        return firstAirDate;
    }

    public Integer displayRuntime() {
        if (runtime != null) {
            return runtime;
        }

        if (episodeRunTime == null || episodeRunTime.isEmpty()) {
            return null;
        }

        return episodeRunTime.getFirst();
    }

    public record TmdbGenreDto(
            Long id,
            String name
    ) {
    }
}