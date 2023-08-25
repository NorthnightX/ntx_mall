package com.ntx.mallorder.controller;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TShipping;
import com.ntx.mallorder.service.TShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shipping")
public class ShippingController {
    @Autowired
    private TShippingService shippingService;

    /**
     * 新增收货地址
     * @return
     */
    @PostMapping("/addShipping")
    public Result addShipping(@RequestBody TShipping shipping){
        return shippingService.addShipping(shipping);
    }

    /**
     * 获取用户的收货地址
     * @return
     */
    @GetMapping("/getShipping")
    public Result getShipping(){
        return shippingService.getShipping();
    }

    /**
     * 删除收货地址
     * @param id
     * @return
     */
    @DeleteMapping("/deleteShipping/{id}")
    public Result deleteShipping(@PathVariable Integer id){
        return shippingService.deleteShipping(id);
    }
}
