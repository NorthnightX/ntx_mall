package com.ntx.mallorder.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.ntx.mallcommon.domain.*;
import com.ntx.mallcommon.feign.CartClient;
import com.ntx.mallcommon.feign.ProductClient;
import com.ntx.mallorder.DTO.OrderDTO;
import com.ntx.mallorder.config.RabbitConfig;
import com.ntx.mallorder.service.TOrderItemService;
import com.ntx.mallorder.service.TOrderService;
import com.ntx.mallorder.service.TPayInfoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class RabbitConsumer {
    @Autowired
    private TOrderService ordersService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private TOrderItemService orderItemService;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private CartClient cartClient;
    @Autowired
    private TPayInfoService payInfoService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
    @Transactional
    public void consume(String msg){
        TOrder tOrder = JSON.parseObject(msg, TOrder.class);
        tOrder.setDeleted(1);
        tOrder.setStatus(20);
        //免邮费
        tOrder.setPostage(0);
        tOrder.setGmtCreate(LocalDateTime.now());
        tOrder.setGmtModified(LocalDateTime.now());
        tOrder.setPaymentTime(LocalDateTime.now());
        tOrder.setCloseTime(LocalDateTime.now());
        tOrder.setSendTime(LocalDateTime.now());
        tOrder.setEndTime(LocalDateTime.now());
        //生成id
        long id = tOrder.getOrderNo();
        //查询购物车
        List<TCart> userCheckedCart = cartClient.getUserCheckedCart(Math.toIntExact(tOrder.getUserId()));
        Map<Long, TCart> cartMap = userCheckedCart.stream().collect(Collectors.toMap(TCart::getProductId, tcart -> tcart));
        //商品id集合
        List<Long> productIdList = userCheckedCart.stream().map(TCart::getProductId).collect(Collectors.toList());
        //获取商品信息
        BigDecimal totalAmount = BigDecimal.ZERO;
        //商品信息集合
        List<TProduct> product = productClient.getProduct(productIdList);
        //遍历商品计算总金额
        List<TOrderItem> list = new ArrayList<>();
        for (TProduct tProduct : product) {
            Long productId = tProduct.getId();
            TCart tCart = cartMap.get(productId);
            //计算当前商品的金额
            BigDecimal quantity = BigDecimal.valueOf(tCart.getQuantity());
            BigDecimal price = tProduct.getPrice();
            BigDecimal productTotal = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
            //设置商品详情
            TOrderItem orderItem = new TOrderItem();
            orderItem.setOrderNo(id);
            orderItem.setUserId(tOrder.getUserId());
            orderItem.setProductId(tProduct.getId());
            orderItem.setProductName(tProduct.getName());
            orderItem.setProductImage(tProduct.getMainImage());
            orderItem.setQuantity(tCart.getQuantity());
            orderItem.setTotalPrice(productTotal);
            orderItem.setCurrentUnitPrice(productTotal);
            orderItem.setDeleted(1);
            orderItem.setStatus(1);
            orderItem.setGmtCreate(LocalDateTime.now());
            orderItem.setGmtModified(LocalDateTime.now());
            orderItemService.save(orderItem);
            list.add(orderItem);
            //总金额计算
            totalAmount = totalAmount.add(productTotal);
        }
        //设置总金额
        tOrder.setPayment(totalAmount);
        ordersService.save(tOrder);
        //设置支付信息
        TPayInfo tPayInfo = new TPayInfo();
        tPayInfo.setUserId(tOrder.getUserId());
        tPayInfo.setOrderNo(id);
        tPayInfo.setPayPlatform(Long.valueOf(tOrder.getPaymentType()));
        tPayInfo.setPlatformNumber(UUID.randomUUID().toString());
        tPayInfo.setPlatformStatus("已支付");
        tPayInfo.setStatus(1);
        tPayInfo.setDeleted(1);
        tPayInfo.setGmtCreate(LocalDateTime.now());
        tPayInfo.setGmtModified(LocalDateTime.now());
        payInfoService.save(tPayInfo);
        //移除用户的购物车信息
        cartClient.removeUserCart(tOrder.getUserId());
        //设置OrderDTO
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderItemList(list);
        BeanUtil.copyProperties(tOrder, orderDTO);
        if(orderDTO.getPaymentType() == 1){
            orderDTO.setPaymentTypeName("微信");
        }
        else {
            orderDTO.setPaymentTypeName("支付宝");
        }
        orderDTO.setStatusName("已支付");
        mongoTemplate.save(orderDTO);
    }
}
