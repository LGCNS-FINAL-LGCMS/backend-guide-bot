package com.lgcms.backendguidebot.remote.core;

import com.lgcms.backendguidebot.common.dto.BaseResponse;
import com.lgcms.backendguidebot.remote.core.dto.FaqResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "RemoteFaqService",
        path = "/api/internal/core/faq"
)
public interface RemoteFaqService {
    @GetMapping(value="")
    public BaseResponse<List<FaqResponse>> getFaq();
}
