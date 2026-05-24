package com.dept.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbSearchResponse(
        int page,
        List<TmdbMovieDto> results,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_results") int totalResults
) {
}