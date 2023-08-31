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

    /**
     * 支付订单
     * @param order
     * @return
     */
    @PostMapping("/payOrder")
    public Result payOrder(@RequestBody TOrder order){
        return orderService.payOrder(order);
    }

    /**
     * 查询订单
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/myOrder")
    public Result myOrder(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                          @RequestParam(required = false, defaultValue = "5") Integer pageSize){
        return orderService.myOrder(pageNum, pageSize);
    }

    /**
     * 管理员分页查询
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/queryAll")
    public Result queryAll(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                           @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) String productName){
        return orderService.queryAll(pageNum, pageSize, status, productName);
    }

    @DeleteMapping("/deleteOrder")
    public Result deleteOrder(@RequestParam Long orderId){
        return orderService.deleteOrder(orderId);
    }




}
