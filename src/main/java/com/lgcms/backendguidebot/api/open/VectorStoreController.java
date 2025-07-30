package com.lgcms.backendguidebot.api.open;

import com.lgcms.backendguidebot.common.dto.BaseResponse;
import com.lgcms.backendguidebot.domain.dto.FaqResponse;
import com.lgcms.backendguidebot.remote.core.RemoteFaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/open/vector/")
@RequiredArgsConstructor
public class VectorStoreController {
    private final RemoteFaqService remoteFaqService;

    @GetMapping("/ingest")
    public ResponseEntity<BaseResponse<String>> ingest() {
        // 1. 코어에서 Open-feign으로 faq데이터를 가져온다.
        //      우선은 postgresql에 더미데이터를 넣어두고그것을 사용한다. 후에 수정예정
        // 2. 가져온 데이터를 임베딩하여 pgvector에 저장한다.
        List<FaqResponse> originalData = remoteFaqService.getFaq().data();


        return ResponseEntity.ok(BaseResponse.ok("코어로부터 데이터를 가져와 임베딩해 저장했습니다."));
    }
}
