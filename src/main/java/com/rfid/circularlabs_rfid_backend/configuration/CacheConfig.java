/**
package com.rfid.circularlabs_rfid_backend.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class CacheConfig {

    public static final String CACHE1 = "cache1";
    public static final String CACHE2 = "cache2";

    @AllArgsConstructor
    @Getter
    public static class CacheProperty {
        private String name;
        private Integer ttl;
    }

    // Redis Cache 설정
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {

        List<CacheProperty> properties = new ArrayList<>();
        properties.add(new CacheProperty(CACHE1, 300));
        properties.add(new CacheProperty(CACHE2, 30));

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType(Object.class)
                .build();

        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // 혹시라도 의도치 않거나 알 수 없는 정보가 들어와 시리얼라이즈를 할 수 없게 될 경우를 대비한 설정값
                .registerModule(new JavaTimeModule())
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
                .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS); // redis를 활용할 객체들에 날짜 정보가 TimeStamp 형식으로 적용되어있을 경우 그대로 RedisTemplate을 사용하면 에러가 발생하므로 그것에 대비하기 위한 설정값

        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return (builder -> {
            properties.forEach(property -> {
                builder.withCacheConfiguration(
                        property.getName(),
                        RedisCacheConfiguration
                                .defaultCacheConfig()
                                .disableCachingNullValues()
                                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer))
                                .entryTtl(Duration.ofSeconds(property.getTtl()))
                );
            });
        });
    }

}
**/