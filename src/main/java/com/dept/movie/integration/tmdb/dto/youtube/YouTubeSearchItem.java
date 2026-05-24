package com.dept.movie.integration.tmdb.dto.youtube;

public record YouTubeSearchItem(
        YouTubeVideoId id,
        Snippet snippet
) {
    public record Snippet(
            String title,
            String channelTitle
    ) {
    }
}