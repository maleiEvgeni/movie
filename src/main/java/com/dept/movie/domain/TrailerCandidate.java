package com.dept.movie.domain;

public record TrailerCandidate(
        TrailerProvider provider,
        String url,
        String source,
        boolean official,
        String name
) {
}