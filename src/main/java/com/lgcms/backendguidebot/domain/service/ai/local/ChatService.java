package com.lgcms.backendguidebot.domain.service.ai.local;


import com.lgcms.backendguidebot.domain.advisor.QueryExpansionAdvisor;
import com.lgcms.backendguidebot.domain.advisor.ReRankAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatService {
    private final ChatClient.Builder chatClientBuilder;
    private final QueryExpansionAdvisor queryExpansionAdvisor;
    private final ReRankAdvisor reRankAdvisor;


    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore
            , QueryExpansionAdvisor queryExpansionAdvisor, ReRankAdvisor reRankAdvisor) {
        this.chatClientBuilder = chatClientBuilder;
        this.queryExpansionAdvisor = queryExpansionAdvisor;
        this.reRankAdvisor = reRankAdvisor;
    }



    public String getResponse(String userQuery) {

        Prompt initialPrompt = new Prompt(userQuery);

        ChatClient chatClient = chatClientBuilder.build();
        OpenAiChatOptions openAiChatOptionsLLM = OpenAiChatOptions.builder()
                .model("gpt-4o")
                .temperature(0.3)
                .maxCompletionTokens(1000)
                .topP(0.7)
                .build();


        return chatClient.prompt(initialPrompt)
                .advisors(queryExpansionAdvisor, reRankAdvisor)
                .options(openAiChatOptionsLLM)
                .call()
                .content();
    }
}