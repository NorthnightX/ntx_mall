package com.ntx.mallcommon.feign;

import com.ntx.mallcommon.domain.TProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("mallUserService")
public interface UserClient {
    @GetMapping("/user/getUserName")
    String getUserName(@RequestParam Integer userId);
}
