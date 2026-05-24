package com.dept.movie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    @Bean
    GenericJacksonJsonRedisSerializer redisJsonSerializer(ObjectMapper objectMapper) {
        return new GenericJacksonJsonRedisSerializer(objectMapper);
    }
}