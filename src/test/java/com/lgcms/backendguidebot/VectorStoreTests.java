package com.lgcms.backendguidebot;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
class VectorStoreTests {

    @Autowired
    private VectorStore vectorStore;

    @Test
    public void pgvectorTest() {
        System.out.println("테스트시동");
        List<Document> documents = List.of(
                new Document("dkjkfsjiklkmmlsf", Map.of("meta1", "meta1")),
                new Document("콭텐츸ㅋ"),
                new Document("앞으로걸어나가는 작은거인", Map.of("meta2", "meta2")));

        vectorStore.add(documents);

        List<Document> results = vectorStore.similaritySearch(SearchRequest
                .builder().query("거인")
                .build());

        for (Document document : Objects.requireNonNull(results)) {
            System.out.println(document);
        }
    }

}
