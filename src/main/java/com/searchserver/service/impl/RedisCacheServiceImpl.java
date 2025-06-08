package com.searchserver.service.impl;

import com.searchserver.model.LaptopInfo;
import com.searchserver.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    private final RedisTemplate<String, LaptopInfo> redisTemplate;
    @Autowired
    public RedisCacheServiceImpl(RedisTemplate<String, LaptopInfo> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isConnected() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
