package com.dept.movie.api.dto;

import java.time.LocalDate;

public record MovieSummaryResponse(
        String id,
        Long tmdbId,
        String title,
        String overview,
        LocalDate releaseDate,
        String posterUrl,
        String backdropUrl,
        Double rating,
        String type,
        TrailerResponse trailer
) {
}