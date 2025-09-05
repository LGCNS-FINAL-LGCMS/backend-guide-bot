package com.lgcms.backendguidebot.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String lockKey();

    // 락 기다리는 시간
    long waitTime() default 5L;

    // 락 실제점유하고 있는시간
    long leaseTime() default 10L;
}