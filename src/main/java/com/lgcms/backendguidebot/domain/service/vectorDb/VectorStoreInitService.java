package com.lgcms.backendguidebot.domain.service.vectorDb;

import com.lgcms.backendguidebot.common.annotation.DistributedLock;
import com.lgcms.backendguidebot.remote.core.RemoteFaqService;
import com.lgcms.backendguidebot.remote.core.dto.FaqResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreInitService {
    private final RemoteFaqService remoteFaqService;
    private final VectorStoreService vectorStoreService;
    private final RedisTemplate<String, String> redisTemplate;

    // 분산락을 위한 키. 분산락을 위한 레디스 db번호는 7번이다.
    private static final String LOCK_KEY = "vector-store-lock";

    // 처음 서버 띄어질시 실행
    @PostConstruct
    @DistributedLock(lockKey = LOCK_KEY)
    public void init() throws InterruptedException {
        try{
//            Thread.sleep(Duration.ofMinutes(3));
            // 우선 vectorDB가 채워져있는지 확인
            // 있다면 하지 않는다. 토큰을 아끼기 위함
            if (vectorStoreService.isEmptyVectorStore()) {
                log.info("core서버에서 faq데이터를 가져옵니다...");
                List<FaqResponse> originalData = remoteFaqService.getFaq().data();
                log.info("가져온 데이터를 임베딩중입니다...");
                vectorStoreService.ingestDataFromList(originalData);
                log.info("성공적으로 벡터DB에 저장했습니다...");
            }else{
                log.info("벡터DB에 이미 데이터가 존재하여 데이터를 가져오지 않습니다.");
            }
        } catch(Exception e){
                log.error("실패했습니다. : {}", e.getMessage());
        }
    }
}
