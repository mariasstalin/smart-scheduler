package com.smartscheduler.common.service;

import com.smartscheduler.common.dto.ZohoToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ZohoTokenService {

    private final RedisTemplate<String, ZohoToken> redisTemplate;

    public ZohoTokenService(@Qualifier("zohoTokenRedisTemplate") RedisTemplate<String, ZohoToken> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getAccessToken() {
        ZohoToken token = redisTemplate.opsForValue().get("zoho:access_token");
        if (token == null) {
            throw new RuntimeException("Access token not found in Redis");
        }
        return token.getAccessToken();
    }
}


