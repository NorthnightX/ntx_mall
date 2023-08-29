package com.ntx.mallcommon.feign;

import com.ntx.mallcommon.domain.TOrder;
import com.ntx.mallcommon.domain.TProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("mallOrderService")
public interface OrderClient {
    @GetMapping("/orderItem/getOrderByProduct")
    Boolean getOrder(@RequestParam int id);
}
