package com.dept.movie.api.dto;

import java.util.List;

public record MovieSearchResponse(
        String query,
        int page,
        int totalPages,
        int totalResults,
        List<MovieSummaryResponse> results
) {
}