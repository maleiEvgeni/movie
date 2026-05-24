package com.dept.movie.cache;

import com.dept.movie.config.AppProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class TwoLevelCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final Cache<String, Object> searchL1;
    private final Cache<String, Object> detailsL1;
    private final Cache<String, Object> trailerL1;

    private final AppProperties properties;

    public TwoLevelCacheService(
            org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory,
            GenericJacksonJsonRedisSerializer redisJsonSerializer,
            AppProperties properties
    ) {
        this.properties = properties;

        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(connectionFactory);
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setValueSerializer(redisJsonSerializer);
        this.redisTemplate.afterPropertiesSet();

        this.searchL1 = Caffeine.newBuilder()
                .expireAfterWrite(properties.cache().searchL1Ttl())
                .maximumSize(10_000)
                .build();

        this.detailsL1 = Caffeine.newBuilder()
                .expireAfterWrite(properties.cache().detailsL1Ttl())
                .maximumSize(20_000)
                .build();

        this.trailerL1 = Caffeine.newBuilder()
                .expireAfterWrite(properties.cache().trailerL1Ttl())
                .maximumSize(50_000)
                .build();
    }

    public <T> T getOrLoad(String cacheName, String key, Class<T> type, Supplier<T> loader) {
        String fullKey = cacheName + ":" + key;

        Cache<String, Object> l1 = l1(cacheName);
        Duration l2Ttl = l2Ttl(cacheName);

        Object l1Value = l1.getIfPresent(fullKey);
        if (type.isInstance(l1Value)) {
            return type.cast(l1Value);
        }

        Object l2Value = redisTemplate.opsForValue().get(fullKey);
        if (type.isInstance(l2Value)) {
            l1.put(fullKey, l2Value);
            return type.cast(l2Value);
        }

        T loaded = loader.get();

        if (loaded != null) {
            l1.put(fullKey, loaded);
            redisTemplate.opsForValue().set(fullKey, loaded, l2Ttl);
        }

        return loaded;
    }

    public <T> Optional<T> get(String cacheName, String key, Class<T> type) {
        String fullKey = cacheName + ":" + key;

        Cache<String, Object> l1 = l1(cacheName);

        Object l1Value = l1.getIfPresent(fullKey);
        if (type.isInstance(l1Value)) {
            return Optional.of(type.cast(l1Value));
        }

        Object l2Value = redisTemplate.opsForValue().get(fullKey);
        if (type.isInstance(l2Value)) {
            l1.put(fullKey, l2Value);
            return Optional.of(type.cast(l2Value));
        }

        return Optional.empty();
    }

    public void put(String cacheName, String key, Object value) {
        if (value == null) {
            return;
        }

        String fullKey = cacheName + ":" + key;

        l1(cacheName).put(fullKey, value);
        redisTemplate.opsForValue().set(fullKey, value, l2Ttl(cacheName));
    }

    private Cache<String, Object> l1(String cacheName) {
        return switch (cacheName) {
            case CacheNames.SEARCH -> searchL1;
            case CacheNames.DETAILS -> detailsL1;
            case CacheNames.TRAILER -> trailerL1;
            default -> throw new IllegalArgumentException("Unknown cache: " + cacheName);
        };
    }

    private Duration l2Ttl(String cacheName) {
        return switch (cacheName) {
            case CacheNames.SEARCH -> properties.cache().searchL2Ttl();
            case CacheNames.DETAILS -> properties.cache().detailsL2Ttl();
            case CacheNames.TRAILER -> properties.cache().trailerL2Ttl();
            default -> throw new IllegalArgumentException("Unknown cache: " + cacheName);
        };
    }
}