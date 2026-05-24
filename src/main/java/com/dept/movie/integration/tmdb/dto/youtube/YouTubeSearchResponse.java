package com.dept.movie.integration.tmdb.dto.youtube;

import java.util.List;

public record YouTubeSearchResponse(
        List<YouTubeSearchItem> items
) {
}