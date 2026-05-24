package com.dept.movie.integration.tmdb.dto;

import java.util.List;

public record TmdbVideosResponse(
        Long id,
        List<TmdbVideoDto> results
) {
}
