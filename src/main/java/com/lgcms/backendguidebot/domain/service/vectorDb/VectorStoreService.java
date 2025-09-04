package com.lgcms.backendguidebot.domain.service.vectorDb;

import com.lgcms.backendguidebot.remote.core.dto.FaqResponse;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class VectorStoreService {
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;


    public VectorStoreService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }


    // 실제 사용하는 openfeign으로 가져온 list<faqresponse>를 임베딩해 저장하는 함수
    public void ingestDataFromList(List<FaqResponse> FaqList) {
        jdbcTemplate.execute("TRUNCATE TABLE guide_bot_embedded_q");
        log.info("삭제");
        long beforetime = System.currentTimeMillis();

        List<Document> documents = new ArrayList<>();
        FaqList
                .forEach(faq -> {
                    Map<String, Object> metadata = new HashMap<>(Map.of(
                            "originalAnswer", faq.answer(),
                            "createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    ));
                    if (faq.url() != null) {
                        metadata.put("url", faq.url());
                    }
                    if (faq.image_url() != null) {
                        metadata.put("image_url", faq.image_url());
                    }

                    Document document = new Document(faq.question(), metadata);
                    documents.add(document);
                });
        // 임베딩해 저장
        vectorStore.add(documents);

        long afteretime = System.currentTimeMillis();
        log.info(String.valueOf((afteretime - beforetime) / 1000));
    }
}
