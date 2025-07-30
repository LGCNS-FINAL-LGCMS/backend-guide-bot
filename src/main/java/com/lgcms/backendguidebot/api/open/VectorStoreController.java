package com.lgcms.backendguidebot.api.open;

import com.lgcms.backendguidebot.common.dto.BaseResponse;
import com.lgcms.backendguidebot.domain.dto.FaqResponse;
import com.lgcms.backendguidebot.domain.service.vectorDb.VectorStoreService;
import com.lgcms.backendguidebot.remote.core.RemoteFaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/open/vector/")
@RequiredArgsConstructor
public class VectorStoreController {
    private final RemoteFaqService remoteFaqService;
    private final VectorStoreService vectorStoreService;

    // 코어에서faq데이터를 가져오고 그것을 임베딩해 저장한다.
    @GetMapping("/ingest")
    public ResponseEntity<BaseResponse<String>> ingest() {
//        List<FaqResponse> originalData = remoteFaqService.getFaq().data();

//        vectorStoreService.ingestDataFromList(originalData);

        vectorStoreService.ingestDataFromJson();
        return ResponseEntity.ok(BaseResponse.ok("코어로부터 데이터를 가져와 임베딩해 저장했습니다."));
    }
}
