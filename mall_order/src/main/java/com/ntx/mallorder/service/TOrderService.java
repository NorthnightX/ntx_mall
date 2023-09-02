package com.ntx.mallorder.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TOrder;

/**
* @author NorthnightX
* @description 针对表【t_order】的数据库操作Service
* @createDate 2023-08-21 21:55:07
*/
public interface TOrderService extends IService<TOrder> {

    Result payOrder(TOrder order);


    Result myOrder(Integer pageNum, Integer pageSize);

    Result queryAll(Integer pageNum, Integer pageSize, Integer status, String productName);

    Result deleteOrder(Long orderId);

    Result payForOrder(TOrder order);

    Result getPayMethodRate();

    Result orderStatusRate();

    Result send(Long orderNo);

    Result harvest(Long orderNo);

    Result refund(Long orderNo);

    ;
}
