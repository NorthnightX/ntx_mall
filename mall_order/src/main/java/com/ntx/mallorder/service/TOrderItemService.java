package com.ntx.mallorder.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.TOrderItem;

/**
* @author NorthnightX
* @description 针对表【t_order_item】的数据库操作Service
* @createDate 2023-08-21 21:55:10
*/
public interface TOrderItemService extends IService<TOrderItem> {

    Boolean getOrderByProduct(int id);
}
