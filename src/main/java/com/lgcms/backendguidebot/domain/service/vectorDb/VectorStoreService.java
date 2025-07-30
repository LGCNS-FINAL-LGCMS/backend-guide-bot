package com.lgcms.backendguidebot.domain.service.vectorDb;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcms.backendguidebot.domain.dto.FaqResponse;
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
import java.util.HashMap;
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

    @Value("classpath:/product_faq.json")
    private Resource productFaq;

    public VectorStoreService(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
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
     * json파일을 읽어서 document로 변환한다.
     * 임베딩된 q가 데이터로 저장되고 원본q,a, created_at이 메타데이터로 저장된다.
     */
    public List<Document> readDataFromJson() {
        // 1. objectmapper로 Q와 A를 구분하며 메타데이터에 넣는다. jsonReader를 안쓰는 이유는 메타데이터에 못넣음...
        List<rawDataRecord> tempDocuments;

        try (InputStream inputStream = productFaq.getInputStream()) {
            tempDocuments = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("벡터스토어서비스 objectmapper실패");
            throw new RuntimeException(e);
        }

        // 2. metadata 만들고 documents구성하기
        List<Document> documents = tempDocuments
                .stream()
                .map(doc -> {
                    String q = (String) doc.question();
                    String a = (String) doc.answer();

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


    // 실제 사용하는 openfeign으로 가져온 list<faqresponse>를 임베딩해 저장하는 함수
    public void ingestDataFromList(List<FaqResponse> FaqList) {
        long beforetime = System.currentTimeMillis();

        List<Document> documents = new ArrayList<>();
        FaqList
                .forEach(faq -> {
                    Map<String, Object> metadata = Map.of(
                            "originalAnswer", faq.answer(),
                            "createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    );

                    Document document = new Document(faq.question(), metadata);
                    documents.add(document);
                });
        // 임베딩해 저장
        vectorStore.add(documents);

        long afteretime = System.currentTimeMillis();
        log.info(String.valueOf((afteretime - beforetime) / 1000));
    }
}
