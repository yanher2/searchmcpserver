package com.searchserver.repository.impl;

import com.searchserver.config.MilvusConfig;
import com.searchserver.model.LaptopInfo;
import com.searchserver.repository.LaptopInfoRepository;
import io.milvus.client.MilvusClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import io.milvus.param.dml.DeleteParam;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class MilvusLaptopInfoRepository implements LaptopInfoRepository {

    private final MilvusClient milvusClient;
    private final RedisTemplate<String, LaptopInfo> redisTemplate;
    private final MilvusConfig milvusConfig;
    private static final String REDIS_KEY_PREFIX = "laptop:";
    private static final String ID_COUNTER_KEY = "laptop:id:counter";

    public MilvusLaptopInfoRepository(
            MilvusClient milvusClient,
            RedisTemplate<String, LaptopInfo> redisTemplate,
            MilvusConfig milvusConfig) {
        this.milvusClient = milvusClient;
        this.redisTemplate = redisTemplate;
        this.milvusConfig = milvusConfig;
    }

    @PostConstruct
    public void init() {
        createCollectionIfNotExists();
        createIndexIfNotExists();
    }

    private void createCollectionIfNotExists() {
        // 检查集合是否存在
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .build();
        R<Boolean> hasCollection = milvusClient.hasCollection(hasCollectionParam);

        if (hasCollection.getData()) {
            return;
        }

        // 创建字段
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();

        FieldType embeddingField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(milvusConfig.getTextDimension())
                .build();

        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withDescription("Laptop information collection")
                .addFieldType(idField)
                .addFieldType(embeddingField)
                .build();

        milvusClient.createCollection(createCollectionParam);
    }

    private void createIndexIfNotExists() {
        // 创建索引
        IndexType indexType = IndexType.valueOf(milvusConfig.getIndexType());
        MetricType metricType = MetricType.valueOf(milvusConfig.getMetricType());
        
        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withFieldName("embedding")
                .withIndexType(indexType)
                .withMetricType(metricType)
                .withExtraParam("{\"nlist\":1024}")
                .build();

        milvusClient.createIndex(createIndexParam);

        // 加载集合到内存
        LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .build();
        milvusClient.loadCollection(loadCollectionParam);
    }

    @Override
    public LaptopInfo save(LaptopInfo laptopInfo) {
        if (laptopInfo.getId() == null) {
            Long id = redisTemplate.opsForValue().increment(ID_COUNTER_KEY);
            laptopInfo.setId(id);
        }

        // 保存到Redis
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + laptopInfo.getId(), laptopInfo);

        // 保存向量到Milvus
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", Collections.singletonList(laptopInfo.getId())));
        fields.add(new InsertParam.Field("embedding", Collections.singletonList(laptopInfo.getEmbedding())));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withFields(fields)
                .build();

        milvusClient.insert(insertParam);

        return laptopInfo;
    }

    @Override
    public Optional<LaptopInfo> findById(Long id) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + id));
    }

    @Override
    public List<LaptopInfo> findAll() {
        Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        return redisTemplate.opsForValue().multiGet(keys).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        redisTemplate.delete(REDIS_KEY_PREFIX + id);
        // Milvus删除操作（注意：Milvus 2.x版本支持删除操作）
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withExpr("id in [" + id + "]")
                .build();
        milvusClient.delete(deleteParam);
    }

    @Override
    public boolean existsById(Long id) {
        return redisTemplate.hasKey(REDIS_KEY_PREFIX + id);
    }

    @Override
    public Optional<LaptopInfo> findByProductId(String productId) {
        return findAll().stream()
                .filter(laptop -> laptop.getProductId().equals(productId))
                .findFirst();
    }

    @Override
    public List<LaptopInfo> findByBrand(String brand) {
        return findAll().stream()
                .filter(laptop -> laptop.getBrand().equalsIgnoreCase(brand))
                .collect(Collectors.toList());
    }

    @Override
    public List<LaptopInfo> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return findAll().stream()
                .filter(laptop -> laptop.getPrice().compareTo(minPrice) >= 0
                        && laptop.getPrice().compareTo(maxPrice) <= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<LaptopInfo> findSimilarLaptops(double[] embedding, int limit) {
        MetricType metricType = MetricType.valueOf(milvusConfig.getMetricType());
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withMetricType(metricType)
                .withOutFields(Collections.singletonList("id"))
                .withTopK(limit)
                .withVectors(Collections.singletonList(embedding))
                .withVectorFieldName("embedding")
                .build();

        R<SearchResults> searchResponse = milvusClient.search(searchParam);
        if (searchResponse.getStatus() != R.Status.Success.getCode()) {
            return Collections.emptyList();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResponse.getData().getResults());
        List<Long> ids = wrapper.getIDScore(0).stream()
                .map(id -> Long.parseLong(id.toString()))
                .collect(Collectors.toList());

        return ids.stream()
                .map(id -> redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<LaptopInfo> searchByKeyword(String keyword) {
        String lowercaseKeyword = keyword.toLowerCase();
        return findAll().stream()
                .filter(laptop ->
                    containsIgnoreCase(laptop.getBrand(), lowercaseKeyword) ||
                    containsIgnoreCase(laptop.getModel(), lowercaseKeyword) ||
                    containsIgnoreCase(laptop.getTitle(), lowercaseKeyword) ||
                    containsIgnoreCase(laptop.getDescription(), lowercaseKeyword))
                .collect(Collectors.toList());
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
    }

    @Override
    public List<LaptopInfo> saveAll(List<LaptopInfo> laptops) {
        return laptops.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        // 删除Redis中的所有数据
        Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 删除Milvus集合中的所有数据
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withExpr("1==1")
                .build();
        milvusClient.delete(deleteParam);
    }

    @Override
    public boolean isConnected() {
        try {
            R<Boolean> milvusCheck = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                    .withCollectionName(milvusConfig.getCollectionName())
                    .build()
            );
            return milvusCheck.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            return false;
        }
    }
}
