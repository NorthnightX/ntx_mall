package com.ntx.mallcommon.feign;

import com.ntx.mallcommon.domain.TProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient("mallProductService")
public interface ProductClient {

    @GetMapping("/product/getCartProduct")
    List<TProduct> getProduct(@RequestParam List<Long> productIdList);

    @PostMapping("/product/updateProductStock")
    Boolean updateProductStock(Map<Long, Integer> map);

    @PostMapping("/product/productStockRollback")
    Boolean productStockRollback(Map<Long, Integer> map);
}
