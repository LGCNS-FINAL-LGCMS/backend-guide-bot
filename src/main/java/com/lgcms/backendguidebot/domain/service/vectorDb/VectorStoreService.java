package com.lgcms.backendguidebot.domain.service.vectorDb;

import com.lgcms.backendguidebot.remote.core.dto.FaqResponse;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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



    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // 데이터가 있는지 여부를 확인하는 함수
    public void checkDatabase(){
        SearchRequest request = SearchRequest.builder()
                .query("아")
                .similarityThreshold(0)
                .build();
        List<Document> result = vectorStore
                .similaritySearch(request);

        if(result.isEmpty()){
            log.info("삭제할데이터가 없습니다.");
            return;
        }
        List<String> resultIds = result.stream()
                .map(Document::getId)
                .toList();
        vectorStore.delete(resultIds);
        log.info("vectordb가 채워져있어 일괄삭제 후 데이터를 가져옵니다.");
        return;
    }

    // 실제 사용하는 openfeign으로 가져온 list<faqresponse>를 임베딩해 저장하는 함수
    public void ingestDataFromList(List<FaqResponse> FaqList) {
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
