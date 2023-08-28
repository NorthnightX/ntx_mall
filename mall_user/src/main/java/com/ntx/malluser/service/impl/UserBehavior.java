package com.ntx.malluser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallcommon.domain.UserActive;
import com.ntx.mallcommon.feign.ProductClient;
import com.ntx.malluser.pojo.DTO.ActiveDTO;
import com.ntx.malluser.service.UserActiveService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserBehavior {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserActiveService userActiveService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ProductClient productClient;
    @KafkaListener(topics = "userBehavior", groupId = "userBehavior")
    public void userBehavior(ConsumerRecord<String, String> record){
        String value = record.value();
        UserActive userActive = JSON.parseObject(value, UserActive.class);
        userActiveService.save(userActive);
        ActiveDTO activeDTO = new ActiveDTO();
        BeanUtil.copyProperties(userActive, activeDTO);
        ArrayList<Long> list = new ArrayList<>();
        list.add(Long.valueOf(activeDTO.getProductId()));
        List<TProduct> product =
                productClient.getProduct(list);
        for (TProduct tProduct : product) {
            activeDTO.setProductName(tProduct.getName());
            activeDTO.setProductPrice(tProduct.getPrice());
            activeDTO.setProductImage(tProduct.getMainImage());
        }
        mongoTemplate.save(activeDTO);
    }
}
