package com.lgcms.backendguidebot.remote.core;

import com.lgcms.backendguidebot.common.dto.BaseResponse;
import com.lgcms.backendguidebot.domain.dto.FaqResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "RemoteFaqService", url="http://localhost:8080", path = "/faq")
public interface RemoteFaqService {
    @GetMapping(value="")
    BaseResponse<List<FaqResponse>> getFaq();
}
