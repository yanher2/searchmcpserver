package com.searchserver.service;

import org.springframework.stereotype.Service;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class HealthCheckService implements HealthIndicator {

    private final MilvusLaptopInfoRepository milvusRepository;
    private final RedisCacheService redisCacheService;

    @Autowired
    public HealthCheckService(MilvusLaptopInfoRepository milvusRepository,
                           RedisCacheService redisCacheService) {
        this.milvusRepository = milvusRepository;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public Health health() {
        boolean milvusHealthy = checkMilvusConnection();
        boolean redisHealthy = checkRedisConnection();

        if (milvusHealthy && redisHealthy) {
            return Health.up()
                    .withDetail("Milvus", "Available")
                    .withDetail("Redis", "Available")
                    .build();
        } else {
            return Health.down()
                    .withDetail("Milvus", milvusHealthy ? "Available" : "Unavailable")
                    .withDetail("Redis", redisHealthy ? "Available" : "Unavailable")
                    .build();
        }
    }

    private boolean checkMilvusConnection() {
        try {
            return milvusRepository.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedisConnection() {
        try {
            return redisCacheService.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}
