package com.lgcms.backendguidebot.api.open;

import com.lgcms.backendguidebot.common.dto.BaseResponse;
import com.lgcms.backendguidebot.domain.dto.ChatResponse;
import com.lgcms.backendguidebot.domain.service.ai.local.ChatService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@Slf4j
@RequestMapping("/guide")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;


    @PostMapping("")
    public ResponseEntity<BaseResponse<ChatResponse>> askQuestion(
            @RequestBody ChatRequest chatRequest
    ) {
        ChatResponse response = chatService.getResponse(chatRequest.query);
        log.info("완료");
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<String>>> getRecommendWords(){
        List<String> response = chatService.getRecommendWord();
        log.info("추천키워드 제공완료");
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private String query;
    }
}