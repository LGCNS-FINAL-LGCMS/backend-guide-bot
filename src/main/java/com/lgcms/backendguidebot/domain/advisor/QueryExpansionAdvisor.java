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
                .maxCompletionTokens(500)
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 이 곳이 내부 로직
        String userQuery = chatClientRequest.prompt().getContents();
        ChatClient expansionClient = chatClientBuilder.build();

        PromptTemplate expansionPrompt = new PromptTemplate(
                """
                        당신은 사용자의 원본 쿼리를 기반으로, 검색(retrieval)을 위한 최적의 확장 쿼리 키워드/구문을 생성하는 AI 도우미입니다.
                        원본 쿼리의 핵심적인 질문 의도를 유지하면서, 관련성 높은 키워드와 구문을 만들어주세요.
                        불필요하게 광범위한 주제나 원본 쿼리의 직접적인 의도를 벗어나는 내용은 포함하지 마세요.
                        오직 원본 쿼리와 의미적으로 매우 유사하거나, 동일한 정보를 찾기 위해 사용될 수 있는 다양한 표현들을 생성해야 합니다.
                        결과는 쉼표로 구분된 키워드/구문 목록 형태로 제공해주세요.
                        
                        ---
                        Original query: 강의 구매 어케함
                        Expanded query keywords/phrases: 강의 구매 방법, 온라인 강의 구매, 강의 수강 신청 절차, 강의 결제 방법
                        
                        Original query: 환불 절차 알려줘
                        Expanded query keywords/phrases: 환불 방법, 결제 취소 절차, 수강료 환불 안내, 구매 취소 방법, 환불 신청
                        
                        Original query: 강의 수료증 어디서 받나요?
                        Expanded query keywords/phrases: 수료증 발급처, 강의 수료증 발급, 수료증 수령 방법, 수료증 신청 장소, 수료증 출력
                        
                        ---
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