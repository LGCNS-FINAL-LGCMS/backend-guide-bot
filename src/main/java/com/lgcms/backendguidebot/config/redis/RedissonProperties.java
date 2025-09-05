package com.lgcms.backendguidebot.config.redis;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@ConfigurationProperties(prefix = "spring.data.redis.config")
@NoArgsConstructor
public class RedissonProperties {
    private int connectionPoolSize;
    private int connectionMinimumIdleSize;
    private int subscriptionConnectionPoolSize;
    private int subscriptionConnectionMinimumIdleSize;
    private int idleConnectionTimeout;
    private int connectTimeout;
    private int timeout;
}