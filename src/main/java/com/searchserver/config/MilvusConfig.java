package com.searchserver.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
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

    @Value("${spring.milvus.text-dimension}")
    private Integer textDimension;

    @Value("${spring.milvus.image-dimension}")
    private Integer imageDimension;

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

    public Integer getTextDimension() {
        return textDimension;
    }

    public Integer getImageDimension() {
        return imageDimension;
    }



    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setTextDimension(Integer textDimension) {
        this.textDimension = textDimension;
    }

    public void setImageDimension(Integer imageDimension) {
        this.imageDimension = imageDimension;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }
}
