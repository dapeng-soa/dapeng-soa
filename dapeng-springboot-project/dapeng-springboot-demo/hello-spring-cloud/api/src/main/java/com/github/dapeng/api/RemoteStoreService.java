package com.github.dapeng.api;

import com.github.dapeng.api.dto.StoreRequest;
import com.github.dapeng.api.dto.StoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(
        contextId = "remoteStoreService",
        value = "dapeng-sc-demo")
public interface RemoteStoreService {

    @RequestMapping("/store")
    StoreResponse store(StoreRequest request);
}
