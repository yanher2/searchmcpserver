package com.searchserver.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Value("${spring.milvus.host}")
    private String host;

    @Value("${spring.milvus.port}")
    private Integer port;

    @Value("${spring.milvus.collection-name}")
    private String collectionName;

    @Value("${spring.milvus.dimension}")
    private Integer dimension;

    @Value("${spring.milvus.index-type}")
    private String indexType;

    @Value("${spring.milvus.metric-type}")
    private String metricType;

    @Bean
    public MilvusClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();
        return new MilvusServiceClient(connectParam);
    }

    public String getCollectionName() {
        return collectionName;
    }

    public Integer getDimension() {
        return dimension;
    }

    public String getIndexType() {
        return indexType;
    }

    public String getMetricType() {
        return metricType;
    }
}
