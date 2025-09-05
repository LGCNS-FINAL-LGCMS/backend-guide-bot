package com.lgcms.backendguidebot.domain.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class QueryExpansionAdvisor implements CallAdvisor {
    private final ChatClient.Builder chatClientBuilder;

    public QueryExpansionAdvisor(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 이 곳이 내부 로직
        ChatOptions chatOptions = ChatOptions.builder()
                .model("anthropic.claude-3-haiku-20240307-v1:0")
                .build();

        String userQuery = chatClientRequest.prompt().getContents();
        ChatClient expansionClient = chatClientBuilder
                .defaultOptions(chatOptions)
                .build();

        PromptTemplate expansionPrompt = new PromptTemplate(
                """
                        You are an AI assistant that generates up to 4 optimal expanded query keywords/phrases for retrieval based on the user's original query.
                        Generate only Korean outputs that strictly preserve the core intent of the original query while covering close paraphrases and synonym variants.
                        Do not include broad or tangential topics. Remove duplicates and filler words.
                        Prefer short, retrieval‑friendly phrases (2~8어절), mixing natural phrases and concise keyword forms.
                        If the query expresses “about/intro/purpose/function of a site/service/platform,” include synonyms like 소개, 목적, 제공 서비스, 기능, 주요 특징 등으로 변형하되 의도를 벗어나지 마세요.
                        Output must be a single comma-separated list with no numbering or extra text. No trailing punctuation.
                        
                        Special rules:
                        
                        If the query is a greeting, return only: 안녕하세요!
                        If the meaningful query length is ≤ 1 character or not understandable, return only: 잘모르겠습니다.
                        Maximum 4 expansions. If fewer are truly relevant, return fewer.
                        Original query: {query}
                        Expanded query keywords/phrases:
                        """
        );

        log.info("원본쿼리 : {}", userQuery);
        Prompt queryInPrompt = expansionPrompt.create(Map.of("query", userQuery));


        String expandedQuery = expansionClient.prompt(queryInPrompt)
                .call()
                .content();

        log.info("확장쿼리 : {}", expandedQuery);

        // 확장쿼리를 다시 ChatClientRequest안에다가 넣는다.
        List<Message> messages = new ArrayList<>(chatClientRequest.prompt().getInstructions());
        messages.add(0, new SystemMessage("expanded query : " + expandedQuery));

        Prompt newPrompt = new Prompt(messages, chatClientRequest.prompt().getOptions());
        ChatClientRequest newChatClientRequest = ChatClientRequest.builder()
                .prompt(newPrompt)
                .build();

        // 다 하고 난 뒤 어드 바이저 혹은 마지막 llm 호출을 실시한다.
        return callAdvisorChain.nextCall(newChatClientRequest);
    }

    @Override
    public String getName() {
        return "QueryExpansionAdvisor";
    }

    // 낮으면 순서도 우선이라 먼저실행
    @Override
    public int getOrder() {
        return 10;
    }
}