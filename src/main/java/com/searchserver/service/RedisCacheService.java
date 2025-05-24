package com.searchserver.service;

public interface RedisCacheService {
    /**
     * 检查Redis连接状态
     * @return 如果连接正常返回true，否则返回false
     */
    boolean isConnected();
}
