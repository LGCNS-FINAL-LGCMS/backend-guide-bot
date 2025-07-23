package com.lgcms.backendguidebot;

import com.lgcms.backendguidebot.domain.service.vectorDb.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackendGuideBotApplicationTests {

    private VectorStoreService vectorStoreService;
    public void setVectorStoreService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }
    @Test
    void contextLoads() {
        System.out.println("테스트시동");

        vectorStoreService.ingestDataFromJson();
    }
}
