package com.lgcms.backendguidebot.domain.advisor;

import com.lgcms.backendguidebot.common.dto.exception.BaseException;
import com.lgcms.backendguidebot.common.dto.exception.QnaError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReRankAdvisor implements CallAdvisor {
    private final ChatClient.Builder chatClientBuilder;
    private final OpenAiChatOptions reRankChatOptions;
    private final VectorStore vectorStore;
    @Value("classpath:prompts/rag-prompt.st")
    private Resource ragPromptTemplateResource;

    public ReRankAdvisor(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClientBuilder = chatClientBuilder;
        this.vectorStore = vectorStore;
        reRankChatOptions = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.6)
                .build();
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
                .topK(20)
                .build()
        );
        if (Objects.requireNonNull(retrievedDocuments).isEmpty()) {
            throw new BaseException(QnaError.QNA_SERVER_ERROR);
        }
        log.info("검색결과 : {}", retrievedDocuments);

        // re-rank
        ChatClient reRankClient = chatClientBuilder.build();

        String documentsMetadata = retrievedDocuments.stream()
                .map(doc -> {
                    StringBuilder docInfo = new StringBuilder();
                    docInfo.append("Document ID: ").append(doc.getId()).append("\n");
                    docInfo.append("Metadata: ").append(doc.getMetadata()).append("\n");

                    // 메타데이터 추출 및 추가
                    Map<String, Object> metadata = doc.getMetadata();
                    if (metadata.containsKey("원본q")) {
                        docInfo.append("원본q: ").append(metadata.get("원본q")).append("\n");
                    }
                    if (metadata.containsKey("원본a")) {
                        docInfo.append("원본a: ").append(metadata.get("원본a")).append("\n");
                    }
                    if (metadata.containsKey("생성일자")) {
                        docInfo.append("생성일자: ").append(metadata.get("생성일자")).append("\n");
                    }
                    return docInfo.toString();
                })
                .collect(Collectors.joining("\n\n"));


        PromptTemplate reRankPromptTemplate = new PromptTemplate(
                """
                        당신은 리랭크를 도와주는 도우미입니다. 사용자쿼리와 document리스트를 줄테니,
                        쿼리 유사도를 기반으로 document를 re-rank하세요.
                        각 document는 orginalQ(원본Q), originalA(원본A), createdAt(생성일자)를 metadata안에 가지고 있습니다.
                        모든 정보를 고려해서, 특히 생성일자를 신경쓰면서 원본Q가 사용자쿼리와 얼마나 매치하는지를 중점으로
                        re-ranked Documents를 제공하세요. '--Document--'로 구분지으세요.
                        
                        User Query : {query}
                        
                        Documents: {documents}
                        
                        Re-ranked Documents :
                        """
        );
        Prompt reRankInPrompt = reRankPromptTemplate.create(
                Map.of("query", userQuery, "documents", documentsMetadata)
        );

        String reRankedContent = reRankClient.prompt(reRankInPrompt)
                .options(reRankChatOptions)
                .call()
                .content();

        List<Message> finalMessageList = new ArrayList<>();
        finalMessageList.add(new SystemMessage(ragPromptTemplateResource + reRankedContent));
        finalMessageList.add(new UserMessage(userQuery));

        Prompt finalReRankPrompt = new Prompt(finalMessageList, chatClientRequest
                .prompt()
                .getOptions());
        ChatClientRequest finalChatClient = ChatClientRequest.builder()
                .prompt(finalReRankPrompt)
                .build();

//        System.out.println("안녕"+reRankedContent);
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