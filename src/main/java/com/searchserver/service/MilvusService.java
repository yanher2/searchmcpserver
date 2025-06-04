package com.searchserver.service;

import com.searchserver.config.MilvusConfig;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MilvusService {

    @Autowired
    private MilvusConfig milvusConfig;

    private MilvusServiceClient milvusClient;

    @PostConstruct
    public void init() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(milvusConfig.getHost())
                .withPort(milvusConfig.getPort())
                .build();
        milvusClient = new MilvusServiceClient(connectParam);

        createCollectionIfNotExists();
        createIndexIfNotExists();
        loadCollection();
    }

    @PreDestroy
    public void cleanup() {
        if (milvusClient != null) {
            milvusClient.releaseCollection(ReleaseCollectionParam.newBuilder()
                    .withCollectionName(milvusConfig.getCollectionName())
                    .build());
            milvusClient.close();
        }
    }

    private void createCollectionIfNotExists() {
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType textVectorField = FieldType.newBuilder()
                .withName("text_vector")
                .withDataType(DataType.FloatVector)
                .withDimension(milvusConfig.getTextDimension())
                .build();

        FieldType imageVectorField = FieldType.newBuilder()
                .withName("image_vector")
                .withDataType(DataType.FloatVector)
                .withDimension(milvusConfig.getImageDimension())
                .build();

        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withDescription("Laptop product search collection")
                .addFieldType(idField)
                .addFieldType(textVectorField)
                .addFieldType(imageVectorField)
                .build();

        milvusClient.createCollection(createCollectionParam);
    }

    private void createIndexIfNotExists() {
        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withFieldName("text_vector")
                .withIndexType(IndexType.valueOf(milvusConfig.getIndexType()))
                .withMetricType(MetricType.valueOf(milvusConfig.getMetricType()))
                .build();

        milvusClient.createIndex(createIndexParam);
    }

    private void loadCollection() {
        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .build());
    }

    public void insertTextVector(List<Float> textVector) {
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("text_vector", List.of(textVector)));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withFields(fields)
                .build();

        milvusClient.insert(insertParam);
    }

    public void insertImageVector(List<Float> imageVector) {
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("image_vector", List.of(imageVector)));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withFields(fields)
                .build();

        milvusClient.insert(insertParam);
    }

    public List<SearchResultsWrapper.IDScore> searchByTextVector(List<Float> textVector, int topK) {
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withVectorFieldName("text_vector")
                .withVectors(List.of(textVector))
                .withTopK(topK)
                .withMetricType(MetricType.valueOf(milvusConfig.getMetricType()))
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        return wrapper.getIDScore(0).stream().toList();
    }

    public List<SearchResultsWrapper.IDScore> searchByImageVector(List<Float> imageVector, int topK) {
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withVectorFieldName("image_vector")
                .withVectors(List.of(imageVector))
                .withTopK(topK)
                .withMetricType(MetricType.valueOf(milvusConfig.getMetricType()))
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        SearchResults results = response.getData();
        SearchResultsWrapper wrapper = new SearchResultsWrapper(results.getResults());
        return wrapper.getIDScore(0).stream().toList();
    }
}
