package com.xypu.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtils {

    public static final String CHECK_CODE_CLIENT_PREFIX = "check_code:client:";
    public static final String CHECK_CODE_ADMIN_PREFIX = "check_code:admin:";
    public static final String AUTH_TOKEN_PREFIX = "auth:token:";

    public static final long CHECK_CODE_TTL = 300L;
    public static final long TOKEN_TTL = 604800L;
    public static final long TOKEN_REFRESH_THRESHOLD = 86400L;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public void expire(String key, long ttlSeconds) {
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }
}
