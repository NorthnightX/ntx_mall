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
import com.ntx.mallorder.DTO.RateDTO;
import com.ntx.mallorder.DTO.UserHolder;
import com.ntx.mallorder.config.RabbitConfig;
import com.ntx.mallorder.mapper.TOrderMapper;
import com.ntx.mallorder.service.TOrderItemService;
import com.ntx.mallorder.service.TOrderService;
import com.ntx.mallorder.service.TPayInfoService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    @Autowired
    private TPayInfoService payInfoService;
    @Autowired
    private TOrderMapper orderMapper;

    /**
     * 生成订单
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
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_TTL_EXCHANGE, RabbitConfig.ORDER_TTL_KEY, jsonString);
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
        try {
            Page<OrderDTO> page = new Page<>(pageNum, pageSize);
            Long id = UserHolder.getUser().getId();
            Query query = new Query();
            query.addCriteria(Criteria.where("userId").is(id));
            long count = mongoTemplate.count(query, OrderDTO.class);
            query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
            query.with(Sort.by(Sort.Direction.DESC, "gmtCreate"));
            List<OrderDTO> orderDTOS = mongoTemplate.find(query, OrderDTO.class);
            page.setTotal(count);
            page.setRecords(orderDTOS);
            return Result.success(page);
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 查询订单
     * @param pageNum
     * @param pageSize
     * @param status
     * @param productName
     * @return
     */
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

    /**
     * 删除订单
     * @param orderId
     * @return
     */
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

    /**
     * 支付订单
     * @param order
     * @return
     */
    @Override
    @Transactional
    public Result payForOrder(TOrder order) {
        LambdaQueryWrapper<TOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TOrder::getOrderNo, order.getOrderNo());
        TOrder tOrder = this.getOne(queryWrapper);
        if(tOrder == null){
            return Result.error("该订单不存在");
        }
        Integer status = tOrder.getStatus();
        if(status == 0){
            return Result.error("该订单已超时取消");
        }
        if(status == 20){
            return Result.error("该订单已支付");
        }
        if(status == 10){
            //支付处理
            tOrder.setPaymentTime(LocalDateTime.now());
            tOrder.setEndTime(LocalDateTime.now());
            tOrder.setGmtModified(LocalDateTime.now());
            tOrder.setCloseTime(LocalDateTime.now());
            //假数据
            tOrder.setSendTime(LocalDateTime.now());
            //设置订单已支付
            tOrder.setStatus(20);
            this.updateById(tOrder);
            //设置支付信息
            TPayInfo tPayInfo = new TPayInfo();
            tPayInfo.setUserId(tOrder.getUserId());
            tPayInfo.setOrderNo(tOrder.getOrderNo());
            tPayInfo.setPayPlatform(Long.valueOf(tOrder.getPaymentType()));
            tPayInfo.setPlatformNumber(UUID.randomUUID().toString());
            tPayInfo.setPlatformStatus("已支付");
            tPayInfo.setStatus(1);
            tPayInfo.setDeleted(1);
            tPayInfo.setGmtCreate(LocalDateTime.now());
            tPayInfo.setGmtModified(LocalDateTime.now());
            payInfoService.save(tPayInfo);
            //更新Mongodb
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(tOrder.getOrderNo()));
            Update update = new Update();
            update.set("status", 20);
            update.set("gmtModified", LocalDateTime.now());
            update.set("statusName", "已支付");
            update.set("paymentTime", LocalDateTime.now());
            update.set("endTime", LocalDateTime.now());
            update.set("closeTime", LocalDateTime.now());
            update.set("sendTime", LocalDateTime.now());
            mongoTemplate.updateFirst(query, update, OrderDTO.class);
        }
        return Result.success("支付成功");
    }

    @Override
    public Result getPayMethodRate() {
        List<RateDTO> payMethodRate = orderMapper.getPayMethodRate();
        for (RateDTO rateDTO : payMethodRate) {
            String name = rateDTO.getName();
            if(name.equals("1")){
                rateDTO.setName("微信");
            }
            else{
                rateDTO.setName("支付宝");
            }
        }
        return Result.success(payMethodRate);
    }

    @Override
    public Result orderStatusRate() {
        List<RateDTO> orderStatusRate = orderMapper.orderStatusRate();
        for (RateDTO rateDTO : orderStatusRate) {
            String name = rateDTO.getName();
            if(name.equals("0")){
                rateDTO.setName("已取消");
            } else if (name.equals("10")) {
                rateDTO.setName("待支付");
            } else if (name.equals("20")) {
                rateDTO.setName("已支付");
            }
        }
        return Result.success(orderStatusRate);
    }

    @Override
    public Result send(Long orderNo) {
        LambdaQueryWrapper<TOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TOrder::getOrderNo, orderNo);
        TOrder tOrder = this.getOne(queryWrapper);
        if(tOrder.getStatus() == 40){
            return Result.error("商品已经发货");
        }
        if(tOrder.getStatus() == 20){
            this.update().set("status", 40).set("send_time", LocalDateTime.now()).eq("order_no", orderNo).update();
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(orderNo));
            Update update = new Update();
            update.set("sendTime", LocalDateTime.now());
            update.set("status", 40);
            update.set("statusName", "已发货");
            mongoTemplate.updateFirst(query, update, OrderDTO.class);
            return Result.success("发货成功");
        }
        else {
            return Result.error("订单已取消或未付款");
        }
    }

    @Override
    public Result harvest(Long orderNo) {
        LambdaQueryWrapper<TOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TOrder::getOrderNo, orderNo);
        TOrder tOrder = this.getOne(queryWrapper);
        if(tOrder.getStatus() == 40){
            this.update().set("status", 50).set("end_time", LocalDateTime.now()).eq("order_no", orderNo).update();
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(orderNo));
            Update update = new Update();
            update.set("endTime", LocalDateTime.now());
            update.set("status", 50);
            update.set("statusName", "已收货");
            mongoTemplate.updateFirst(query, update, OrderDTO.class);
            return Result.success("收获成功");
        }
        if(tOrder.getStatus() == 20){
            return Result.error("商品还未发货");
        }
        else {
            return Result.error("订单已取消或未付款");
        }
    }
}




