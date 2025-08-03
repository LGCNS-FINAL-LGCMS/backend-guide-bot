package com.lgcms.backendguidebot.domain.dto;

public record FaqResponse(Long id, String Q, String A
        , String url, String image_url, String category) {

}
