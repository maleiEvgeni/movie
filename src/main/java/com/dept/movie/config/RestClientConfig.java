package com.dept.movie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient tmdbRestClient(AppProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.tmdb().connectTimeout());
        requestFactory.setReadTimeout(properties.tmdb().readTimeout());

        return RestClient.builder()
                .baseUrl(properties.tmdb().baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + properties.tmdb().apiKey())
                .build();
    }

    @Bean
    RestClient youtubeRestClient(AppProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.youtube().connectTimeout());
        requestFactory.setReadTimeout(properties.youtube().readTimeout());

        return RestClient.builder()
                .baseUrl(properties.youtube().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}