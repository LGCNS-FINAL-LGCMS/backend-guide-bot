package com.lgcms.backendguidebot.domain.service.vectorDb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreInitRunner implements ApplicationRunner {

    private final VectorStoreInitService vectorStoreInitService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("ApplicationRunner 시작: VectorStoreInit 호출합니다.");
        // 다른 Bean(VectorStoreInitService)의 메소드를 호출하므로
        // AOP가 정상적으로 동작합니다.
        vectorStoreInitService.init();
    }
}