package com.lgcms.backendguidebot.domain.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/pgv/v1")
public class VectorStoreController {
    private final VectorStore vectorStore;
    @PostMapping("/")
    public ResponseEntity<List<Document>> accessTest(@RequestBody SearchRequest searchRequest) {

        return ResponseEntity.ok(vectorStore.similaritySearch(
                searchRequest
        ));
    }
}
