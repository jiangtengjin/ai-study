package com.aistudy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class KnowledgeBaseVectorStoreService {

    private final EmbeddingModel embeddingModel;
    private final String storePath;
    private final Map<Long, SimpleVectorStore> storeCache = new ConcurrentHashMap<>();
    /** 每个知识库中 documentId -> 该文档的所有向量块 */
    private final Map<Long, Map<Long, List<Document>>> docIndexCache = new ConcurrentHashMap<>();

    public KnowledgeBaseVectorStoreService(
            EmbeddingModel embeddingModel,
            @Value("${vectorstore.store-path:./data/vectorstore}") String storePath) {
        this.embeddingModel = embeddingModel;
        this.storePath = storePath;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(storePath));
    }

    /**
     * 获取指定知识库的 VectorStore（懒加载 + 缓存）
     */
    private SimpleVectorStore getStore(Long knowledgeBaseId) {
        return storeCache.computeIfAbsent(knowledgeBaseId, id -> {
            File storeFile = getStoreFile(id);
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            if (storeFile.exists() && storeFile.length() > 0) {
                try {
                    store.load(storeFile);
                    rebuildDocIndex(id, store);
                    log.info("已加载知识库 {} 的向量数据", id);
                } catch (Exception e) {
                    log.warn("加载知识库 {} 向量数据失败，将使用空存储: {}", id, e.getMessage());
                }
            }
            return store;
        });
    }

    /**
     * 从已加载的 store 中重建 documentId 索引
     */
    private void rebuildDocIndex(Long knowledgeBaseId, SimpleVectorStore store) {
        Map<Long, List<Document>> index = new ConcurrentHashMap<>();
        List<Document> allDocs = store.similaritySearch(
                SearchRequest.builder().query("").topK(100000).similarityThreshold(0.0).build());
        for (Document doc : allDocs) {
            Object docIdMeta = doc.getMetadata().get("documentId");
            if (docIdMeta != null) {
                try {
                    long docId = Long.parseLong(docIdMeta.toString());
                    index.computeIfAbsent(docId, k -> new ArrayList<>()).add(doc);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        docIndexCache.put(knowledgeBaseId, index);
    }

    /**
     * 添加文档块到指定知识库
     */
    public void addDocuments(Long knowledgeBaseId, List<Document> documents) {
        SimpleVectorStore store = getStore(knowledgeBaseId);
        store.add(documents);

        // 更新索引
        Map<Long, List<Document>> index = docIndexCache.computeIfAbsent(
                knowledgeBaseId, k -> new ConcurrentHashMap());
        for (Document doc : documents) {
            Object docIdMeta = doc.getMetadata().get("documentId");
            if (docIdMeta != null) {
                try {
                    long docId = Long.parseLong(docIdMeta.toString());
                    index.computeIfAbsent(docId, k -> new ArrayList<>()).add(doc);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        persistStore(knowledgeBaseId, store);
        log.info("已向知识库 {} 添加 {} 个文档块", knowledgeBaseId, documents.size());
    }

    /**
     * 从指定知识库中检索相关文档
     */
    public List<Document> search(Long knowledgeBaseId, String query, int topK, double threshold) {
        SimpleVectorStore store = getStore(knowledgeBaseId);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();
        return store.similaritySearch(request);
    }

    /**
     * 删除指定知识库中某文档的所有向量（基于索引，无需全量重建）
     */
    public void deleteByDocumentId(Long knowledgeBaseId, Long documentId) {
        Map<Long, List<Document>> index = docIndexCache.get(knowledgeBaseId);
        if (index == null || !index.containsKey(documentId)) {
            log.warn("知识库 {} 中未找到文档 {} 的索引，跳过向量删除", knowledgeBaseId, documentId);
            return;
        }

        // 移除索引中的文档
        List<Document> removed = index.remove(documentId);

        // 从 store 中删除对应的向量块
        SimpleVectorStore store = getStore(knowledgeBaseId);
        List<String> idsToRemove = new ArrayList<>();
        for (Document doc : removed) {
            if (doc.getId() != null) {
                idsToRemove.add(doc.getId());
            }
        }
        if (!idsToRemove.isEmpty()) {
            store.delete(idsToRemove);
        }

        persistStore(knowledgeBaseId, store);
        log.info("已从知识库 {} 删除文档 {} 的 {} 个向量块", knowledgeBaseId, documentId, removed.size());
    }

    /**
     * 删除整个知识库的向量数据
     */
    public void deleteKnowledgeBase(Long knowledgeBaseId) {
        storeCache.remove(knowledgeBaseId);
        docIndexCache.remove(knowledgeBaseId);
        File storeFile = getStoreFile(knowledgeBaseId);
        if (storeFile.exists()) {
            storeFile.delete();
            log.info("已删除知识库 {} 的向量数据文件", knowledgeBaseId);
        }
    }

    private File getStoreFile(Long knowledgeBaseId) {
        return Paths.get(storePath, "kb_" + knowledgeBaseId + ".json").toFile();
    }

    private void persistStore(Long knowledgeBaseId, SimpleVectorStore store) {
        try {
            File storeFile = getStoreFile(knowledgeBaseId);
            store.save(storeFile);
        } catch (Exception e) {
            log.error("持久化知识库 {} 向量数据失败: {}", knowledgeBaseId, e.getMessage());
        }
    }
}
