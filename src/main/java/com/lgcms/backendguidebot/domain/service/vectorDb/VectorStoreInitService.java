package com.lgcms.backendguidebot.domain.service.vectorDb;

import com.lgcms.backendguidebot.remote.core.RemoteFaqService;
import com.lgcms.backendguidebot.remote.core.dto.FaqResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreInitService {
    private final RemoteFaqService remoteFaqService;
    private final VectorStoreService vectorStoreService;

    // 처음 서버 띄어질시 실행
    @PostConstruct
    public void init() throws InterruptedException {
        Thread.sleep(Duration.ofMinutes(3));
        // vectordb가 채워져있으면 삭제 후 다시 채웁니다.
        vectorStoreService.checkDatabase();
        try{
            log.info("core서버에서 faq데이터를 가져옵니다...");
            List<FaqResponse> originalData = remoteFaqService.getFaq().data();
            log.info("가져온 데이터를 임베딩중입니다...");
            vectorStoreService.ingestDataFromList(originalData);

            log.info("성공적으로 벡터DB에 저장했습니다...");
        }catch (Exception e){
            log.error("실패했습니다. : {}",e.getMessage());
        }

    }
}
