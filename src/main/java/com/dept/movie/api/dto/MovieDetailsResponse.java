package com.dept.movie.api.dto;

import java.time.LocalDate;
import java.util.List;

public record MovieDetailsResponse(
        String id,
        Long tmdbId,
        String title,
        String overview,
        LocalDate releaseDate,
        Integer runtime,
        String posterUrl,
        String backdropUrl,
        Double rating,
        List<String> genres,
        String type,
        TrailerResponse trailer
) {
}