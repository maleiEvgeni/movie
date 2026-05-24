package com.dept.movie.api.dto;

public record TrailerResponse(
        String provider,
        String url,
        String source,
        Boolean official
) {
}