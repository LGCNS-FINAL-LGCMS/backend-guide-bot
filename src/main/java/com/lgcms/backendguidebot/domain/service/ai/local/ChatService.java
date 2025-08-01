package com.lgcms.backendguidebot.domain.service.ai.local;


import com.lgcms.backendguidebot.common.dto.exception.BaseException;
import com.lgcms.backendguidebot.common.dto.exception.QnaError;
import com.lgcms.backendguidebot.domain.advisor.QueryExpansionAdvisor;
import com.lgcms.backendguidebot.domain.advisor.ReRankAdvisor;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class ChatService {
    private final ChatClient.Builder chatClientBuilder;
    private final QueryExpansionAdvisor queryExpansionAdvisor;
    private final ReRankAdvisor reRankAdvisor;



    public String getResponse(String userQuery) {

        Prompt initialPrompt = new Prompt(userQuery);

        ChatOptions chatOptions = ChatOptions.builder()
                .model("anthropic.claude-3-haiku-20240307-v1:0")
                .temperature(0.3)
                .topK(50)
                .topP(0.7)
                .maxTokens(512)
                .build();
        ChatClient chatClient = chatClientBuilder
                .defaultOptions(chatOptions)
                .build();


        // api 키 문제시 "답변생성에 문제가 생겼습니다." 를 보냅니다.
        try {
            return chatClient.prompt(initialPrompt)
                    .advisors(queryExpansionAdvisor, reRankAdvisor)
                    .call()
                    .content();
        }catch (Exception e) {
            log.error("api키 부재 : {}",e.getMessage());
            throw new BaseException(QnaError.QNA_SERVER_ERROR);
        }
    }
}