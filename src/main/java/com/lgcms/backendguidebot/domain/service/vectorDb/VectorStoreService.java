package com.lgcms.backendguidebot.domain.service.vectorDb;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcms.backendguidebot.domain.entity.FaqDocumentEntity;
import com.lgcms.backendguidebot.domain.repository.FaqDocumentRepository;
import com.lgcms.backendguidebot.domain.service.embedding.record.rawDataRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VectorStoreService {
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private final FaqDocumentRepository faqDocumentRepository;


    public VectorStoreService(VectorStore vectorStore, ObjectMapper objectMapper, FaqDocumentRepository faqDocumentRepository) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.faqDocumentRepository = faqDocumentRepository;
    }

    /**
     * 서치 리퀘스트를 받아서 그거 기반으로 벡터db에서 검색하기
     */
    public List<Document> search(SearchRequest searchRequest) {
        List<Document> documents = new ArrayList<>();
        try {
            documents = vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return documents;
    }


    /**
     * postgresql에 있는 question과 answer를 document로 변환한다.
     * 임베딩된 q가 데이터로 저장되고 원본q,a, created_at이 메타데이터로 저장된다.
     */
    public List<Document> readDataFromJson() {
        // 1. db에서 불러오기
        List<FaqDocumentEntity> allEntities = faqDocumentRepository.findAll();

        // 2. metadata 만들고 documents구성하기
        List<Document> documents = allEntities
                .stream()
                .map(doc -> {
                    String q = (String) doc.getQuestion();
                    String a = (String) doc.getAnswer();

                    Map<String, Object> metadata = Map.of(
                            "originalQ", q,
                            "originalA", a,
                            "createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    );

                    return new Document(q, metadata);
                }).toList();
        return documents;
    }

    // completablefutre로 배치단위로 하게 하니까 7초걸리던게 6초 혹은 5초나온다.
    public void ingestDataFromJson() {
        long beforetime = System.currentTimeMillis();
        List<Document> documents = readDataFromJson();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 배치사이즈단위로 실행해서 집어넣는다
        int BATCH_SIZE = 70;
        for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
            final List<Document> batch = documents.subList(i, Math.min(documents.size(), i + BATCH_SIZE));

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                vectorStore.add(batch);
            }, executorService);

            futures.add(future);
        }

        // 위 runasync마무리까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long afteretime = System.currentTimeMillis();
        log.info(String.valueOf((afteretime - beforetime) / 1000));

        System.out.println("\n 문서 검색 테스트 --------------------");
        List<Document> searchResults = search(SearchRequest.builder().query("강의를 무료로 볼 수 있나요?").build());

        System.out.println("문서 내용: " + searchResults.getFirst().getFormattedContent());
    }
}
