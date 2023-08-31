package com.ntx.mallorder.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntx.mallcommon.domain.TOrder;
import com.ntx.mallcommon.domain.TOrderItem;
import com.ntx.mallcommon.feign.CartClient;
import com.ntx.mallcommon.feign.ProductClient;
import com.ntx.mallorder.DTO.CustomException;
import com.ntx.mallorder.DTO.OrderDTO;
import com.ntx.mallorder.config.RabbitConfig;
import com.ntx.mallorder.service.TOrderItemService;
import com.ntx.mallorder.service.TOrderService;
import com.ntx.mallorder.service.TPayInfoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderIsPayServiceImpl {
    @Autowired
    private TOrderService ordersService;
    @Autowired
    private TOrderItemService orderItemService;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private MongoTemplate mongoTemplate;
    @RabbitListener(queues = RabbitConfig.ORDER_CHECK_QUEUE)
    @Transactional(rollbackFor= CustomException.class)
    public void isPay(String msg){
        TOrder tOrder = JSON.parseObject(msg, TOrder.class);
        Long orderNo = tOrder.getOrderNo();
        LambdaQueryWrapper<TOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TOrder::getOrderNo, orderNo);
        TOrder order = ordersService.getOne(queryWrapper);
        //如果有效内，订单的状态还是未付款
        if(order.getStatus() == 10){
            //取消订单
            //设置订单状态为已取消
            order.setStatus(0);
            order.setGmtModified(LocalDateTime.now());
            //更新订单
            ordersService.updateById(order);
            LambdaQueryWrapper<TOrderItem> itemLambdaQueryWrapper = new LambdaQueryWrapper<>();
            itemLambdaQueryWrapper.eq(TOrderItem::getOrderNo, orderNo);
            List<TOrderItem> list = orderItemService.list(itemLambdaQueryWrapper);
            Map<Long, Integer> map = new HashMap<>();
            for (TOrderItem orderItem : list) {
                orderItem.setStatus(0);
                orderItem.setGmtModified(LocalDateTime.now());
                //更新订单项
                orderItemService.updateById(orderItem);
                //查找product的数量
                Long productId = orderItem.getProductId();
                Integer quantity = orderItem.getQuantity();
                map.put(productId, quantity);
            }
            Boolean aBoolean = productClient.productStockRollback(map);
            //商品更新不成功
            if(aBoolean != null && !aBoolean){
                    throw new CustomException("商品更新失败");
            }
            //更新mongodb的数据
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(orderNo));
            OrderDTO orderDTO = mongoTemplate.findOne(query, OrderDTO.class);
            if (orderDTO != null) {
                //更新orderItem的数据
                List<TOrderItem> orderItemList = orderDTO.getOrderItemList();
                for (TOrderItem orderItem : orderItemList) {
                    orderItem.setStatus(0);
                    orderItem.setGmtModified(LocalDateTime.now());
                }
                Update update = new Update();
                update.set("orderItemList", orderItemList);
                update.set("gmtModified", LocalDateTime.now());
                update.set("statusName", "已取消");
                update.set("status", 0);
                mongoTemplate.updateFirst(query, update, OrderDTO.class);
            }
        }

    }
}
