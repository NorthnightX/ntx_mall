package com.ntx.mallcommon.feign;

import com.ntx.mallcommon.domain.TCart;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient("mallCartService")
public interface CartClient {

    @GetMapping("/cart/getUserCheckedCart")
    List<TCart> getUserCheckedCart(@RequestParam Integer userId);

    @DeleteMapping("/cart/deleteUserCartAfterPay")
    Boolean removeUserCart(@RequestParam Long userId);
}
