package com.ntx.malluser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallcommon.domain.UserActive;
import com.ntx.mallcommon.feign.ProductClient;
import com.ntx.malluser.pojo.DTO.ActiveDTO;
import com.ntx.malluser.service.UserActiveService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
        //当天对同一商品的浏览，只添加一次浏览记录，后面的只修改时间，后续需要优化
        String value = record.value();
        UserActive userActive = JSON.parseObject(value, UserActive.class);
        LocalDateTime gmtCreate = userActive.getGmtCreate();
        LocalDateTime startOfDay = gmtCreate.with(LocalTime.MIN);
        LocalDateTime endOfDay = gmtCreate.with(LocalTime.MAX);
        LambdaQueryWrapper<UserActive> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserActive::getProductId, userActive.getProductId());
        queryWrapper.eq(UserActive::getUserId, userActive.getUserId());
        queryWrapper.eq(UserActive::getCategoryId, userActive.getCategoryId());
        queryWrapper.between(UserActive::getGmtCreate, startOfDay, endOfDay);
        UserActive active = userActiveService.getOne(queryWrapper);
        //如果当天没有浏览对应的商品
        if(active == null){
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
        if (active != null) {
            active.setGmtCreate(LocalDateTime.now());
            userActiveService.updateById(active);
            Query query =  new Query();
            query.addCriteria(Criteria.where("_id").is(active.getId()));
            Update update = new Update();
            update.set("gmtCreate", LocalDateTime.now());
            mongoTemplate.updateFirst(query, update, ActiveDTO.class);
        }
    }
}
