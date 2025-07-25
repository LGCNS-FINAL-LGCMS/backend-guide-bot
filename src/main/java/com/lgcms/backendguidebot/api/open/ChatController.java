package com.lgcms.backendguidebot.api.open;

import com.lgcms.backendguidebot.common.dto.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/open/guide/")
public class ChatController {
    private final ChatClient.Builder chatClientBuilder;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }


    @PostMapping("/ask")
    public ResponseEntity<BaseResponse<ChatResponse>> askQuestion(
            @RequestBody ChatRequest chatRequest
    ) {
        ChatClient chatClient = chatClientBuilder.build();

        String answer = chatClient.prompt(chatRequest.query)
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
