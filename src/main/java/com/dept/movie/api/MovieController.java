package com.dept.movie.api;

import com.dept.movie.api.dto.MovieDetailsResponse;
import com.dept.movie.api.dto.MovieSearchResponse;
import com.dept.movie.application.MovieSearchService;
import com.dept.movie.domain.MovieType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Validated
public class MovieController {

    private final MovieSearchService movieSearchService;

    @GetMapping("/search")
    public MovieSearchResponse search(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) @Max(500) int page,
            @RequestParam(defaultValue = "en-US") String language
    ) {
        return movieSearchService.search(query, page, language);
    }

    @GetMapping("/{tmdbId}")
    public MovieDetailsResponse movieDetails(
            @PathVariable Long tmdbId,
            @RequestParam(defaultValue = "en-US") String language
    ) {
        return movieSearchService.details(tmdbId, MovieType.MOVIE, language);
    }

    @GetMapping("/tv/{tmdbId}")
    public MovieDetailsResponse tvDetails(
            @PathVariable Long tmdbId,
            @RequestParam(defaultValue = "en-US") String language
    ) {
        return movieSearchService.details(tmdbId, MovieType.TV, language);
    }
}