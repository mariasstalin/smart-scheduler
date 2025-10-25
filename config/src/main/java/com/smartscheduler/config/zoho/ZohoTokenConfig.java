package com.smartscheduler.config.zoho;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class ZohoTokenConfig {

    // 1. Configure RestClient for Zoho API
    @Bean
    public RestClient zohoRestClient(RestClient.Builder builder) {
        return builder.baseUrl("https://accounts.zoho.in/oauth/v2/token").build();
    }

    // 2. Configure RedisTemplate for DTO Serialization
    @Bean
    public RedisTemplate<String, ZohoToken> zohoTokenRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ZohoToken> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // ObjectMapper setup for Instant serialization
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();

        Jackson2JsonRedisSerializer<ZohoToken> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, ZohoToken.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}