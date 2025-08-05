package com.lgcms.backendguidebot.domain.service.ai.local;


import com.lgcms.backendguidebot.common.dto.exception.BaseException;
import com.lgcms.backendguidebot.common.dto.exception.QnaError;
import com.lgcms.backendguidebot.domain.advisor.QueryExpansionAdvisor;
import com.lgcms.backendguidebot.domain.advisor.ReRankAdvisor;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChatService {
    private final ChatClient.Builder chatClientBuilder;
    private final QueryExpansionAdvisor queryExpansionAdvisor;
    private final ReRankAdvisor reRankAdvisor;

    @Value("classpath:prompts/rag-prompt.st")
    private Resource ragPromptTemplateResource;
    private ChatClientRequest chatClientRequest;

    public ChatService(ChatClient.Builder chatClientBuilder, QueryExpansionAdvisor queryExpansionAdvisor, ReRankAdvisor reRankAdvisor) {
        this.chatClientBuilder = chatClientBuilder;
        this.queryExpansionAdvisor = queryExpansionAdvisor;
        this.reRankAdvisor = reRankAdvisor;
    }


    public String getResponse(String userQuery) {

        Prompt initialPrompt = new Prompt(userQuery);

        ChatOptions chatOptions = ChatOptions.builder()
                .model("anthropic.claude-3-haiku-20240307-v1:0")
                .temperature(0.1)
                .topK(20)
                .topP(0.3)
                .maxTokens(512)
                .build();
        ChatClient chatClient = chatClientBuilder
                .defaultOptions(chatOptions)
                .build();

        // 삭제대상 <- advisor안쓸경우의 비교를 위함

//        List<Message> finalMessageList = new ArrayList<>();
//        String ragPromptTemplate;
//        try {
//            ragPromptTemplate = StreamUtils.copyToString(ragPromptTemplateResource.getInputStream(), StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        SystemMessage systemMessage = new SystemMessage(ragPromptTemplate);
//        UserMessage userMessage = new UserMessage(userQuery);
//        Prompt initialPrompt = new Prompt(List.of(systemMessage, userMessage));

        // 테스트용 끝

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