package com.lgcms.backendguidebot.domain.service.ai.local;


import com.lgcms.backendguidebot.common.dto.exception.BaseException;
import com.lgcms.backendguidebot.common.dto.exception.QnaError;
import com.lgcms.backendguidebot.domain.advisor.QueryExpansionAdvisor;
import com.lgcms.backendguidebot.domain.advisor.ReRankAdvisor;
import com.lgcms.backendguidebot.domain.dto.ChatResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.ai.converter.BeanOutputConverter;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ChatService {
    private final ChatClient.Builder chatClientBuilder;
    private final QueryExpansionAdvisor queryExpansionAdvisor;
    private final ReRankAdvisor reRankAdvisor;
    private final VectorStore vectorStore;
    // "answer" 필드의 값만 추출하기 위한 정규식 패턴
    private static final Pattern ANSWER_FIELD_PATTERN = Pattern.compile("\"answer\":\\s*\"(.*?)\"", Pattern.DOTALL);

    public ChatService(ChatClient.Builder chatClientBuilder, QueryExpansionAdvisor queryExpansionAdvisor, ReRankAdvisor reRankAdvisor, VectorStore vectorStore) {
        this.chatClientBuilder = chatClientBuilder;
        this.queryExpansionAdvisor = queryExpansionAdvisor;
        this.reRankAdvisor = reRankAdvisor;
        this.vectorStore = vectorStore;
    }


    public ChatResponse getResponse(String userQuery) {
        BeanOutputConverter<ChatResponse> beanOutputConverter = new BeanOutputConverter<>(ChatResponse.class);
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

        // api 키 문제시 "답변생성에 문제가 생겼습니다." 를 보냅니다.
        try {
            String rawResponse = chatClient.prompt(initialPrompt)
                    .advisors(queryExpansionAdvisor, reRankAdvisor)
                    .call()
                    .content();

            Matcher matcher = ANSWER_FIELD_PATTERN.matcher(rawResponse);
            if (matcher.find()) {
                String originalAnswer = matcher.group(1);
                String escapedAnswer = originalAnswer.replace("\n", "\\n").replace("\r", "");

                // 수정된 answer 필드 값으로 원본 JSON 문자열을 재조립하기
                String finalJson = rawResponse.replace(originalAnswer, escapedAnswer);

                log.info("최종 JSON: {}", finalJson);
                return beanOutputConverter.convert(finalJson);
            } else {
                throw new Exception();
            }
        }catch (Exception e) {
            log.error("api키 부재 : {}",e.getMessage());
            throw new BaseException(QnaError.QNA_SERVER_ERROR);
        }
    }

    public List<String> getRecommendWord(){
        SearchRequest searchRequest = SearchRequest.builder()
                .query("안녕")
                .topK(3)
                .build();
        List<String> results = vectorStore.similaritySearch(searchRequest)
                .stream()
                .map(Document::getText)
                .toList();

        return results;
    }
}