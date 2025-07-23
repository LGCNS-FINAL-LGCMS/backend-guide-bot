package com.lgcms.backendguidebot.domain.service.vectorDb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VectorStoreService {
    private final VectorStore vectorStore;
    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    List<Document> documents = List.of(
            new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
            new Document("The World is Big and Salvation Lurks Around the Corner"),
            new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));

    /**
     * 서치 리퀘스트를 받아서 그거 기반으로 벡터db에서 검색하기
     *
     */
    public List<Document> search(SearchRequest searchRequest) {
        List<Document> documents = new ArrayList<>();

        try{
            documents = vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return documents;
    }




}
