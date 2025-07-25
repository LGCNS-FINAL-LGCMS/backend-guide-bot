package com.lgcms.backendguidebot.domain.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class QueryExpansionAdvisor implements CallAdvisor {
    private final ChatClient.Builder chatClientBuilder;
    private final OpenAiChatOptions expansionChatOptions;

    public QueryExpansionAdvisor(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
        this.expansionChatOptions = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.4)
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 이 곳이 내부 로직
        String userQuery = chatClientRequest.prompt().getContents();
        ChatClient expansionClient = chatClientBuilder.build();

        PromptTemplate expansionPrompt = new PromptTemplate(
                """
                        당신은 더 나은 retrieval을 위한 쿼리의 확장에 도움을 주는 ai도우미에요.
                        사용자의 원본 쿼리를 분석하여, 해당 쿼리가 포함하는 모든 핵심 개념과 질문 의도를 포괄하는
                        여러 개의 관련 키워드와 구문들을 생성해주세요.
                        특히, 여러 질문이나 주제가 복합된 쿼리일 경우, 각 부분을 명확히 다루는 확장 쿼리를 만들어주세요.
                        결과는 쉼표로 구분된 키워드/구문 목록 형태로 제공해주세요.
                        Original query: {query}
                        Expanded query keywords/phrases:
                        """
        );

        log.info("원본쿼리 : {}", userQuery);
        Prompt queryInPrompt = expansionPrompt.create(Map.of("query", userQuery));
        String expandedQuery = expansionClient.prompt(queryInPrompt)
                .options(expansionChatOptions)
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
