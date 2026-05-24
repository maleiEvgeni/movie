package com.dept.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieDto(
        Long id,
        String title,
        String name,
        String overview,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("first_air_date") String firstAirDate,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        @JsonProperty("vote_average") Double voteAverage,
        @JsonProperty("media_type") String mediaType
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
}