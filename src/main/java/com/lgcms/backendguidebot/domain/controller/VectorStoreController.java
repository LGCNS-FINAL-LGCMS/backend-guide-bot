package com.lgcms.backendguidebot.domain.controller;

import com.lgcms.backendguidebot.common.dto.exception.BaseException;
import com.lgcms.backendguidebot.common.dto.exception.QnaError;
import com.lgcms.backendguidebot.domain.service.FaqDocumentService;
import com.lgcms.backendguidebot.domain.service.vectorDb.VectorStoreService;
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
    private final VectorStoreService vectorStoreService;

    private final FaqDocumentService faqDocumentService;

    @PostMapping("/")
    public ResponseEntity<List<Document>> accessTest(@RequestBody SearchRequest searchRequest) {
        vectorStoreService.ingestDataFromJson();
        return ResponseEntity.ok(vectorStore.similaritySearch(
                searchRequest
        ));
    }

    @GetMapping("/go")
    public void go() {
        faqDocumentService.insertFaqDocument();
//        try{
//
//        }catch (Exception e){
//            throw new BaseException(QnaError.QNA_SERVER_ERROR);
//        }
    }
}
