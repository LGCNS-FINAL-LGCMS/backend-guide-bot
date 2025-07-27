package com.lgcms.backendguidebot.api.open;

import com.lgcms.backendguidebot.common.dto.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/open/guide/")
public class ChatController {
    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;


    public ChatController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClientBuilder = chatClientBuilder;
        this.vectorStore = vectorStore;
    }


    @PostMapping("/ask")
    public ResponseEntity<BaseResponse<ChatResponse>> askQuestion(
            @RequestBody ChatRequest chatRequest
    ) {
        ChatClient chatClient = chatClientBuilder.build();
        List<Document> Documents = vectorStore.similaritySearch(chatRequest.query);
        String context = Documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(" "));

        Message systemMessage = new SystemMessage("당신은 친절한 도우미입니다. 이 컨텍스트를 참고해 답변하세요 context : " + context);
        UserMessage userMessage = new UserMessage(chatRequest.query);

        String answer = chatClient.prompt(new Prompt(List.of(systemMessage, userMessage)))
                .call().content();
        return ResponseEntity.ok(BaseResponse.ok(new ChatResponse(answer)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private String query;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponse {
        private String answer;
    }
//    @GetMapping("/stream/ask")
//    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "너는 스트림코드야") String message) {
//        Prompt prompt = new Prompt(new UserMessage(message));
//        return this.openAiChatModel.stream(prompt);
//    }
}