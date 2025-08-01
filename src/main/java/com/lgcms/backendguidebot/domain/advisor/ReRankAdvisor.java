package com.lgcms.backendguidebot.domain.advisor;

import com.lgcms.backendguidebot.common.dto.exception.BaseException;
import com.lgcms.backendguidebot.common.dto.exception.QnaError;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReRankAdvisor implements CallAdvisor {
//    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    @Value("classpath:prompts/rag-prompt.st")
    private Resource ragPromptTemplateResource;

    public ReRankAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {


        // chatClientRequest 속에 있는 확장쿼리 찾기
        String expandedQuery = null;
        List<Message> messageList = new ArrayList<>(chatClientRequest.prompt().getInstructions());
        for (Message message : messageList) {
            expandedQuery = message.getText().substring(17).trim();
            break;
        }
        log.info("리랭크 어드바이저 : {}", expandedQuery);

        String userQuery = chatClientRequest.prompt().getContents();

        // 검색 단계
        List<Document> retrievedDocuments = vectorStore.similaritySearch(SearchRequest.builder()
                .query(expandedQuery)
                .topK(10)
                .build()
        );
        if (Objects.requireNonNull(retrievedDocuments).isEmpty()) {
            throw new BaseException(QnaError.QNA_SERVER_ERROR);
        }
        log.info("검색결과 : {}", retrievedDocuments);

        // 중복 제거
        // 중복제거 로직 (map은 중복안되니까 맵에다가 다 넣어서 제거처리후 리스트로 )
        List<Document> filteredDocuments = new ArrayList<>(retrievedDocuments.stream()
                .collect(Collectors.toMap(doc -> {
                            return doc.getMetadata().get("originalAnswer");
                        },
                        Function.identity(), // document 객체 그 자체가 value입니다.
                        (existing, replacement) -> existing, // 중복 값은 버리기
                        LinkedHashMap::new // 유사도 순서 기억을 위해 링크드해시맵 사용
                ))
                .values());


        // re-rank <<- 이전엔 llm호출을 통해 rerank를 수행했지만 시간이 너무 오래걸려 내부로직으로 변경
        // 유사도 점수 (metadata:score) 와 생성일자(metadata:createdAt)을 고려해서 정렬하게 처리
        // 중복제거와 re-rank를 내부로직으로 변경하여 rerank시간이 40초에서 0초대로 줄어듦

        // 비교기 1 ( 스코어 기반 )
        Comparator<Document> scoreComparator = Comparator.comparing(
                (Document doc) -> (Double) doc.getMetadata().getOrDefault("score", 0.0)
        ).reversed();

        // 비교기 2 ( 생성일자 기반 )
        Comparator<Document> createdAtComparator = Comparator.comparing(
                (Document doc) -> (String) doc.getMetadata().getOrDefault("createdAt", "0000-01-01T00:00:00")
        ).reversed();
        filteredDocuments.sort(scoreComparator.thenComparing(createdAtComparator));
        log.info("필터된 도큐먼트 : {}", filteredDocuments);



        String documentsMetadata = filteredDocuments.stream()
                .map(doc -> {
                    StringBuilder docInfo = new StringBuilder();
                    docInfo.append("Metadata: ").append(doc.getMetadata()).append("\n");

                    // 메타데이터 추출 및 추가
                    Map<String, Object> metadata = doc.getMetadata();
                    if (metadata.containsKey("originalAnswer")) {
                        docInfo.append("originalAnswer: ").append(metadata.get("originalAnswer")).append("\n");
                    }
                    if (metadata.containsKey("createdAt")) {
                        docInfo.append("createdAt: ").append(metadata.get("createdAt")).append("\n");
                    }
                    if (metadata.containsKey("score")) {
                        docInfo.append("score: ").append(metadata.get("score")).append("\n");
                    }
                    return docInfo.toString();
                })
                .collect(Collectors.joining("\n\n"));
        log.info("string된 메타데이터 {}", documentsMetadata);


        List<Message> finalMessageList = new ArrayList<>();
        String ragPromptTemplate;
        try {
            ragPromptTemplate = StreamUtils.copyToString(ragPromptTemplateResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("system template : {}", ragPromptTemplate);
        finalMessageList.add(new SystemMessage(ragPromptTemplate + documentsMetadata));
        finalMessageList.add(new UserMessage(userQuery));

        Prompt finalReRankPrompt = new Prompt(finalMessageList, chatClientRequest
                .prompt()
                .getOptions());
        ChatClientRequest finalChatClient = ChatClientRequest.builder()
                .prompt(finalReRankPrompt)
                .build();

        return callAdvisorChain.nextCall(finalChatClient);
    }

    @Override
    public String getName() {
        return "ReRankAdvisor";
    }

    @Override
    public int getOrder() {
        return 30;
    }
}