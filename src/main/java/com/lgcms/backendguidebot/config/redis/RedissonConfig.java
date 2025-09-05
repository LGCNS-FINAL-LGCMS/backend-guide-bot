package com.lgcms.backendguidebot.config.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfig {
    private final RedissonProperties redissonProperties;

    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.database-lock}")
    private int database;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectionPoolSize(redissonProperties.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(redissonProperties.getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(redissonProperties.getSubscriptionConnectionPoolSize())
                .setSubscriptionConnectionMinimumIdleSize(redissonProperties.getSubscriptionConnectionMinimumIdleSize())
                .setIdleConnectionTimeout(redissonProperties.getIdleConnectionTimeout())
                .setConnectTimeout(redissonProperties.getConnectTimeout())
                .setTimeout(redissonProperties.getTimeout());

        return Redisson.create(config);
    }
}