package com.ntx.mallorder.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.*;
import com.ntx.mallcommon.feign.CartClient;
import com.ntx.mallcommon.feign.ProductClient;
import com.ntx.mallcommon.feign.UserClient;
import com.ntx.mallorder.DTO.OrderDTO;
import com.ntx.mallorder.DTO.UserHolder;
import com.ntx.mallorder.config.RabbitConfig;
import com.ntx.mallorder.mapper.TOrderMapper;
import com.ntx.mallorder.service.TOrderItemService;
import com.ntx.mallorder.service.TOrderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author NorthnightX
 * @description 针对表【t_order】的数据库操作Service实现
 * @createDate 2023-08-21 21:55:07
 */
@Service
public class TOrderServiceImpl extends ServiceImpl<TOrderMapper, TOrder>
        implements TOrderService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private CartClient cartClient;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private TOrderItemService orderItemService;
    @Autowired
    private UserClient userClient;

    /**
     * 订单异步处理，先扣除数据库的库存,开启事务，防止数据在异常时被修改
     *
     * @param order
     * @return
     */
    @Override
    @Transactional
    public Result payOrder(TOrder order) {
        Long userId = UserHolder.getUser().getId();
        try {
            //获取用户的购物车列表
            if (order.getPaymentType() == null) {
                return Result.error("请选择支付方式");
            }
            if (order.getShippingId() == null) {
                return Result.error("请选择收货地址");
            }
            List<TCart> userCheckedCart = cartClient.getUserCheckedCart(Math.toIntExact(userId));
            if (userCheckedCart.size() == 0) {
                return Result.error("请选择要购买的商品");
            }
            Map<Long, TCart> cartMap = userCheckedCart.stream().collect(Collectors.toMap(TCart::getProductId, tcart -> tcart));
            //获取用户的商品列表
            List<Long> productIds = userCheckedCart.stream().map(TCart::getProductId).collect(Collectors.toList());
            List<TProduct> product = productClient.getProduct(productIds);
            Map<Long, Integer> map = new HashMap<>();
            //判断商品还有没有库存
            for (TProduct tProduct : product) {
                //该商品的剩余库存
                Integer stock = tProduct.getStock();
                TCart cart = cartMap.get(tProduct.getId());
                Integer quantity = cart.getQuantity();
                if (stock == 0) {
                    //存在商品没有库存，返回失败
                    return Result.error("商品售空了,请重新选择");
                }
                //库存小于用户购买量
                if (stock - quantity < 0) {
                    return Result.error("库存不足,请重新选择");
                }
                //库存充足，更新库存
                map.put(tProduct.getId(), quantity);
            }
            Boolean aBoolean = productClient.updateProductStock(map);
            if (!aBoolean) {
                return Result.error("商品太火爆了，请稍后购买");
            }
            //商品有库存
            long id = IdUtil.getSnowflake().nextId();
            order.setOrderNo(id);
            order.setUserId(userId);
            String jsonString = JSON.toJSONString(order);
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ORDER_KEY, jsonString);
            return Result.success(String.valueOf(id));
        } catch (RuntimeException e) {
            return Result.error("网络异常");
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 我的订单
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public Result myOrder(Integer pageNum, Integer pageSize) {
        Page<OrderDTO> page = new Page<>(pageNum, pageSize);
        Long id = UserHolder.getUser().getId();
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(id));
        long count = mongoTemplate.count(query, OrderDTO.class);
        query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
        List<OrderDTO> orderDTOS = mongoTemplate.find(query, OrderDTO.class);
        page.setTotal(count);
        page.setRecords(orderDTOS);
        return Result.success(page);
    }

    @Override
    public Result queryAll(Integer pageNum, Integer pageSize, Integer status, String productName) {
        Page<OrderDTO> page = new Page<>(pageNum, pageSize);
        Query query = new Query();
        if(status != null){
            query.addCriteria(Criteria.where("status").is(status));
        }
        if(productName != null && productName.length() > 0){
            LambdaQueryWrapper<TOrderItem> itemLambdaQueryWrapper = new LambdaQueryWrapper<>();
            itemLambdaQueryWrapper.like(TOrderItem::getProductName, productName);
            itemLambdaQueryWrapper.eq(TOrderItem::getDeleted, 1);
            List<Long> collect = orderItemService.list(itemLambdaQueryWrapper).stream().
                    map(TOrderItem::getOrderNo).distinct().collect(Collectors.toList());
            query.addCriteria(Criteria.where("_id").in(collect));
        }
        long count = mongoTemplate.count(query, OrderDTO.class);
        query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
        page.setTotal(count);
        List<OrderDTO> collect = mongoTemplate.find(query, OrderDTO.class).stream().peek(orderDTO -> {
            Long userId = orderDTO.getUserId();
            String userName = userClient.getUserName(Math.toIntExact(userId));
            orderDTO.setUserName(userName);
        }).collect(Collectors.toList());
        page.setRecords(collect);
        return Result.success(page);
    }

    @Override
    @Transactional
    public Result deleteOrder(Long orderId) {
        this.update().
                eq("order_no", orderId).set("deleted", 0).
                set("gmt_modified", LocalDateTime.now()).update();
        orderItemService.update().
                eq("order_no", orderId).set("deleted", 0).
                set("gmt_modified", LocalDateTime.now()).update();
        Query query = new Query();
        query.addCriteria(Criteria.where("orderNo").is(orderId));
        mongoTemplate.remove(query, OrderDTO.class);
        return Result.success("删除成功");
    }
}




