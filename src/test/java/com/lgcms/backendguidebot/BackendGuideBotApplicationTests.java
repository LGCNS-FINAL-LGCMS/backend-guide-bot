package com.lgcms.backendguidebot;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BackendGuideBotApplicationTests {

    @MockitoBean
    private PgVectorStore vectorStore;

    @Test
    void contextLoads() {
        System.out.println(vectorStore);
        System.out.println("테스트시동");
    }

}
