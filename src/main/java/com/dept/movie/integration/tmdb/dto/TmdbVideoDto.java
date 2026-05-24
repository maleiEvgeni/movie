package com.dept.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbVideoDto(
        String name,
        String key,
        String site,
        String type,
        Boolean official,
        @JsonProperty("published_at") String publishedAt
) {
    public boolean isYouTubeTrailer() {
        return "YouTube".equalsIgnoreCase(site)
                && "Trailer".equalsIgnoreCase(type)
                && key != null
                && !key.isBlank();
    }
}