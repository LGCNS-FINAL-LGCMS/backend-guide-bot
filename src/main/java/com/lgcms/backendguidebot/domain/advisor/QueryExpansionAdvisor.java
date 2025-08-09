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
                        You are an AI assistant that generates optimal expanded query keywords/phrases for retrieval based on the user's original query.
                        Please create highly relevant keywords and phrases while maintaining the core intent of the original query.
                        Do not include overly broad topics or content that deviates from the direct intent of the original query.
                        You must generate only semantically very similar or diverse expressions that can be used to find the same information as the original query.
                        Provide the results as a comma-separated list of keywords/phrases.
                        Extensions that deviate from the intent of the question are prohibited.
                        확장 단어는 최대 4개 입니다. 한글자 이하는 확장하지 않고 "잘모르겠습니다." 만 답변합니다.
                        * 만약 인사말이라면 확장하지 않고 "안녕하세요!" 로만 답변합니다.
                  
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