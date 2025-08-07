package com.lgcms.backendguidebot.domain.service.vectorDb;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcms.backendguidebot.remote.core.dto.FaqResponse;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class VectorStoreService {
    private final VectorStore vectorStore;

    @Value("classpath:/product_faq.json")
    private Resource productFaq;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
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
        List<Document> documents = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, String>> qaData = List.of();
        try (InputStream inputStream = productFaq.getInputStream()) {
            qaData = objectMapper.readValue(inputStream,
                    new TypeReference<>() {
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Map<String, String> entry : qaData) {
            String question = entry.get("Q");
            String answer = entry.get("A");

            // Document의 content에 질문을 저장합니다.
            Document document = new Document(question);

            // metadata 맵을 생성하여 원하는 정보를 추가합니다.
            Map<String, Object> metadata = document.getMetadata();
            metadata.put("originalAnswer", answer);
            metadata.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            documents.add(document);
        }
        return documents;
    }

        // completablefutre로 배치단위로 하게 하니까 7초걸리던게 6초 혹은 5초나온다.
        // 근데 aws bedrock은 배치지원을 안해서 그냥 순차적으로한다.
        public void ingestDataFromJson () {
            long beforetime = System.currentTimeMillis();
            List<Document> documents = readDataFromJson();

            // 문서를 하나씩 순차적으로 처리
            for (Document doc : documents) {
                vectorStore.add(List.of(doc));
            }

            long afteretime = System.currentTimeMillis();
            log.info(String.valueOf((afteretime - beforetime) / 1000));

            System.out.println("\n 문서 검색 테스트 --------------------");
            List<Document> searchResults = search(SearchRequest.builder().query("강의를 무료로 볼 수 있나요?").build());

            System.out.println("문서 내용: " + searchResults.getFirst().getFormattedContent());
        }


        // 실제 사용하는 openfeign으로 가져온 list<faqresponse>를 임베딩해 저장하는 함수
        public void ingestDataFromList (List < FaqResponse > FaqList) {
            long beforetime = System.currentTimeMillis();

            List<Document> documents = new ArrayList<>();
            FaqList
                    .forEach(faq -> {
                        Map<String, Object> metadata = new HashMap<>(Map.of(
                                "originalAnswer", faq.A(),
                                "createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        ));
                        if (faq.url() != null) {
                            metadata.put("url", faq.url());
                        }
                        if (faq.image_url() != null) {
                            metadata.put("image_url", faq.image_url());
                        }

                        Document document = new Document(faq.Q(), metadata);
                        documents.add(document);
                    });
            // 임베딩해 저장
            vectorStore.add(documents);

            long afteretime = System.currentTimeMillis();
            log.info(String.valueOf((afteretime - beforetime) / 1000));
        }
    }
