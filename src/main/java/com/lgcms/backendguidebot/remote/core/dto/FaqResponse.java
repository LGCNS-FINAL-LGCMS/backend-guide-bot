package com.lgcms.backendguidebot.remote.core.dto;

public record FaqResponse(
        Long id,
        String question,
        String answer,
        String url,
        String image_url) {
}
