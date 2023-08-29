package com.ntx.mallorder.controller;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.ntx.mallorder.service.TOrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orderItem")
public class OrderItemController {
    @Autowired
    private TOrderItemService orderItemService;

    @GetMapping("/getOrderByProduct")
    public Boolean getOrderByProduct(@RequestParam int id){
        return orderItemService.getOrderByProduct(id);
    }
}
