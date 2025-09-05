package com.lgcms.backendguidebot.common.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TokenMetricsAspect {
    private final MeterRegistry meterRegistry;

    @AfterReturning(value = "execution(* org.springframework.ai.chat.model.ChatModel.call(..))", returning = "result")
    public void recordTokenMetrics(JoinPoint joinPoint, Object result) {
        if(result instanceof ChatResponse chatResponse) {
            Usage usage = chatResponse.getMetadata().getUsage();
            Integer totalTokens = usage.getTotalTokens();

            Counter.builder("ai.tokens")
                    .description("LLM token usage")
                    .register(meterRegistry)
                    .increment(totalTokens);
        }
    }

    @AfterReturning(value = "execution(* org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi.embedding(..))", returning = "result")
    public void recordEmbeddingTokenMetrics(JoinPoint joinPoint, Object result) {
        if(result instanceof TitanEmbeddingBedrockApi.TitanEmbeddingResponse titanEmbeddingResponse) {
            Integer token = titanEmbeddingResponse.inputTextTokenCount();

            Counter.builder("ai.tokens")
                    .description("LLM token usage")
                    .register(meterRegistry)
                    .increment(token);
        }
    }
}