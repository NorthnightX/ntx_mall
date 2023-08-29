package com.ntx.mallorder.controller;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TOrder;
import com.ntx.mallorder.service.TOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private TOrderService orderService;

    @PostMapping("/payOrder")
    public Result payOrder(@RequestBody TOrder order){
        return orderService.payOrder(order);
    }


}
