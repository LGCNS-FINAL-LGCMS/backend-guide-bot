package com.lgcms.backendguidebot.domain.service.embedding.record;

// 16버전부터 생긴 record. setter가 불가능하다. 데이터를 그대로 받아와서 사용하는 캡슐로 사용하기에 적합하다.
public record rawDataRecord(
        String Q, String A
) { }
