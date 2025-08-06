package com.lgcms.backendguidebot.remote.core.dto;

public record FaqResponse(
        Long id,
        String Q,
        String A,
        String url,
        String image_url) {

}
