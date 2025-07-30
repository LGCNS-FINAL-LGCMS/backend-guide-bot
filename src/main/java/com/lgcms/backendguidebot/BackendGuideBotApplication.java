package com.lgcms.backendguidebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class BackendGuideBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendGuideBotApplication.class, args);
    }

}
