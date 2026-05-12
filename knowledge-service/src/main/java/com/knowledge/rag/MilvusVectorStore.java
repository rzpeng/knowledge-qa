package com.knowledge.rag;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.MutationResultWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class MilvusVectorStore {

    private static final String ID_FIELD = "id";
    private static final String CONTENT_FIELD = "content";
    private static final String VECTOR_FIELD = "vector";
    private static final String DOCUMENT_ID_FIELD = "document_id";
    private static final int VECTOR_DIMENSION = 1024;

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;
    private final String collectionName;

    public MilvusVectorStore(@Value("${milvus.collection-name:knowledge_vectors}") String collectionName) {
        this.collectionName = collectionName;
    }

    @PostConstruct
    public void init() {
        if (milvusClient == null) {
            log.warn("Milvus client not available, skipping collection initialization");
            return;
        }
        createCollectionIfNotExists();
    }

    private void createCollectionIfNotExists() {
        if (milvusClient == null) return;

        R<Boolean> hasCollection = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());

        if (hasCollection.getData() == Boolean.TRUE) {
            log.info("Collection {} already exists", collectionName);
            return;
        }

        FieldType idField = FieldType.newBuilder()
                .withName(ID_FIELD)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName(CONTENT_FIELD)
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName(VECTOR_FIELD)
                .withDataType(DataType.FloatVector)
                .withDimension(VECTOR_DIMENSION)
                .build();

        FieldType documentIdField = FieldType.newBuilder()
                .withName(DOCUMENT_ID_FIELD)
                .withDataType(DataType.Int64)
                .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("Knowledge document vectors")
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(contentField)
                .addFieldType(vectorField)
                .addFieldType(documentIdField)
                .build();

        R<RpcStatus> result = milvusClient.createCollection(createParam);
        if (result.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create collection: " + result.getMessage());
        }

        createIndex();
        log.info("Collection {} created successfully", collectionName);
    }

    private void createIndex() {
        if (milvusClient == null) return;

        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(VECTOR_FIELD)
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"nlist\":1024}")
                .withSyncMode(Boolean.TRUE)
                .build();

        R<RpcStatus> result = milvusClient.createIndex(indexParam);
        if (result.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create index: " + result.getMessage());
        }
    }

    public List<Long> insert(List<String> contents, List<float[]> vectors, Long documentId) {
        if (milvusClient == null) {
            log.warn("Milvus not available, skipping insert");
            return Collections.emptyList();
        }
        loadCollection();

        List<List<Float>> vectorList = new ArrayList<>();
        for (float[] vec : vectors) {
            List<Float> floatList = new ArrayList<>();
            for (float f : vec) {
                floatList.add(f);
            }
            vectorList.add(floatList);
        }

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(CONTENT_FIELD, contents));
        fields.add(new InsertParam.Field(VECTOR_FIELD, vectorList));
        fields.add(new InsertParam.Field(DOCUMENT_ID_FIELD, Collections.nCopies(contents.size(), documentId)));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<MutationResult> result = milvusClient.insert(insertParam);
        if (result.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to insert vectors: " + result.getMessage());
        }

        return new MutationResultWrapper(result.getData()).getLongIDs();
    }

    public List<SearchResult> search(float[] queryVector, int topK) {
        if (milvusClient == null) {
            log.warn("Milvus not available, returning empty search results");
            return Collections.emptyList();
        }
        loadCollection();

        List<Float> floatList = new ArrayList<>();
        for (float f : queryVector) {
            floatList.add(f);
        }
        List<List<Float>> vectors = Collections.singletonList(floatList);

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withTopK(topK)
                .withVectors(vectors)
                .withVectorFieldName(VECTOR_FIELD)
                .addOutField(CONTENT_FIELD)
                .addOutField(DOCUMENT_ID_FIELD)
                .build();

        R<SearchResults> result = milvusClient.search(searchParam);
        if (result.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to search vectors: " + result.getMessage());
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(result.getData().getResults());
        List<?> contents = wrapper.getFieldData(CONTENT_FIELD, 0);
        List<?> docIds = wrapper.getFieldData(DOCUMENT_ID_FIELD, 0);
        List<SearchResult> results = new ArrayList<>();

        List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);
        for (int i = 0; i < idScores.size(); i++) {
            SearchResultsWrapper.IDScore score = idScores.get(i);
            String content = (String) contents.get(i);
            Long docId = (Long) docIds.get(i);
            results.add(new SearchResult(content, score.getScore(), docId));
        }

        return results;
    }

    public void deleteByDocumentId(Long documentId) {
        if (milvusClient == null) return;

        String expr = DOCUMENT_ID_FIELD + " == " + documentId;
        milvusClient.delete(DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build());
    }

    private void loadCollection() {
        if (milvusClient == null) return;
        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
    }

    public record SearchResult(String content, double score, Long documentId) {}
}
