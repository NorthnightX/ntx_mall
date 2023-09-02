package com.ntx.mallcart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.mallcart.DTO.CartDTO;
import com.ntx.mallcart.DTO.UserHolder;
import com.ntx.mallcart.mapper.TCartMapper;
import com.ntx.mallcart.service.TCartService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCart;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.mallcommon.feign.ProductClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.ntx.mallcart.common.SystemConstant.USER_CART;
import static com.ntx.mallcart.common.SystemConstant.USER_CART_PRODUCT;

/**
* @author NorthnightX
* @description 针对表【t_cart】的数据库操作Service实现
* @createDate 2023-08-21 21:55:47
*/
@Service
public class TCartServiceImpl extends ServiceImpl<TCartMapper, TCart>
    implements TCartService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 添加到购物车
     * @param cart
     * @return
     */
    @Override
    public Result addToCart(TCart cart) {
        if(cart.getQuantity() <= 0){
            return Result.error("请至少选择一件商品");
        }
        Long productId = cart.getProductId();
        TUser user = UserHolder.getUser();
        Long userId = user.getId();
        String redisKey = USER_CART + userId;
        String redisKeyProduct = USER_CART_PRODUCT + user.getId();
        try {
            //先去查找用户之前有没有将这个物品加进购物车
            Boolean isAdd = stringRedisTemplate.opsForSet().isMember(redisKeyProduct, productId.toString());
            //如果用户之前加进过购物车
            if(Boolean.TRUE.equals(isAdd)){
                //更新数量
                LambdaQueryWrapper<TCart> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TCart::getUserId, user.getId());
                queryWrapper.eq(TCart::getProductId, productId);
                TCart cartOld = this.getOne(queryWrapper);
                Integer quantity = cartOld.getQuantity() + cart.getQuantity();
                cartOld.setQuantity(quantity);
                cartOld.setGmtModified(LocalDateTime.now());
                this.updateById(cartOld);
                //更新mongoDB
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(cartOld.getId()));
                Update update = new Update();
                update.set("quantity", quantity);
                mongoTemplate.updateFirst(query, update, CartDTO.class);
                return Result.success("添加成功");
            }
            //否则新增购物车
            cart.setStatus(1);
            cart.setDeleted(1);
            cart.setChecked(0);
            cart.setGmtModified(LocalDateTime.now());
            cart.setGmtCreate(LocalDateTime.now());
            cart.setUserId(userId);
            this.save(cart);
            stringRedisTemplate.opsForSet().add(redisKey, String.valueOf(cart.getId()));
            stringRedisTemplate.opsForSet().add(redisKeyProduct, String.valueOf(productId));
            //向mongoDB新增
            CartDTO cartDTO = new CartDTO();
            BeanUtil.copyProperties(cart, cartDTO);
            List<TProduct> product = productClient.getProduct(Collections.singletonList(cartDTO.getProductId()));
            product.forEach(product1 -> {
                cartDTO.setProductImage(product1.getMainImage());
                cartDTO.setProductName(product1.getName());
                cartDTO.setProductPrice(product1.getPrice());
                mongoTemplate.save(cartDTO);
            });
            return Result.success("添加成功");
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 获取用户的购物车
     * @return
     */
    @Override
    public Result getCart() {
        TUser user = UserHolder.getUser();
        try {
            Long userId = user.getId();
            String redisKey = USER_CART + userId;
            //根据用户获取用户的购物车id
            Set<String> members = stringRedisTemplate.opsForSet().members(redisKey);
            //用户购物车为空
            if (members == null || members.size() == 0) {
                //返回空集合
                return Result.success(new ArrayList<>());
            }
            //根据redis的信息查询mongoDB
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").in(members));
            List<CartDTO> cartDTOS = mongoTemplate.find(query, CartDTO.class);
            if(cartDTOS.size() > 0){
                return Result.success(cartDTOS);
            }
            //mongoDB没有查询数据库
            LambdaQueryWrapper<TCart> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(TCart::getId, members);
            List<TCart> list = this.list(queryWrapper);
            List<Long> productIdList = list.stream().map(TCart::getProductId).collect(Collectors.toList());
            List<TProduct> product = productClient.getProduct(productIdList);
            Map<Long, TProduct> collect = product.stream().collect(Collectors.toMap(TProduct::getId, product1 -> product1));
            List<CartDTO> cartDTOList = list.stream().map(cart -> {
                CartDTO cartDTO = new CartDTO();
                BeanUtil.copyProperties(cart, cartDTO);
                Long productId = cartDTO.getProductId();
                String productName = collect.get(productId).getName();
                BigDecimal price = collect.get(productId).getPrice();
                cartDTO.setProductPrice(price);
                cartDTO.setProductName(productName);
                cartDTO.setProductImage(collect.get(productId).getMainImage());
                mongoTemplate.save(cartDTO);
                return cartDTO;
            }).collect(Collectors.toList());
            return Result.success(cartDTOList);
        } finally {
            UserHolder.removeUser();
        }
    }


    /**
     * 用户购物车删除
     * @param id
     * @return
     */
    @Override
    public Result deletedFormCart(Integer id) {
        try {
            Long userId = UserHolder.getUser().getId();
            TCart tCart = this.getById(id);
            String redisKey = USER_CART + userId;
            String redisKeyProduct = USER_CART_PRODUCT + userId;
            //根据用户获取用户的购物车id
            //如果是该用户的购物车
            if(Objects.equals(userId, tCart.getUserId())){
                //删除该条数据
                this.removeById(id);
                stringRedisTemplate.opsForSet().remove(redisKey, tCart.getId().toString());
                stringRedisTemplate.opsForSet().remove(redisKeyProduct, tCart.getProductId().toString());
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(tCart.getId()));
                mongoTemplate.remove(query, CartDTO.class);
                return Result.success("移除成功");
            }
            return Result.error("这不是你的购物车");
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 更新用户的选中状态
     * @param cartDTO
     * @return
     */
    @Override
    public Result updateCartCheck(CartDTO cartDTO) {
        Long cartDTOId = cartDTO.getId();
        Integer checked = cartDTO.getChecked();
        this.update().
                eq("id", cartDTOId).set("checked", checked).
                set("gmt_modified", LocalDateTime.now()).update();
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(cartDTOId));
        Update update = new Update();
        update.set("checked", checked);
        mongoTemplate.updateFirst(query, update, CartDTO.class);
        return Result.success("更新成功");

    }

    /**
     * 更新用户的商品数量
     * @param cartDTO
     * @return
     */
    @Override
    public Result updateCartCount(CartDTO cartDTO) {
        Long cartDTOId = cartDTO.getId();
        Integer quantity = cartDTO.getQuantity();
        this.update().
                eq("id", cartDTOId).set("quantity", quantity).
                set("gmt_modified", LocalDateTime.now()).update();
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(cartDTOId));
        Update update = new Update();
        update.set("quantity", quantity);
        mongoTemplate.updateFirst(query, update, CartDTO.class);
        return Result.success("更新成功");
    }

    /**
     * 获取用户需要付款的购物车
     * @return
     */
    @Override
    public Result getCartToPay() {
        Long id = UserHolder.getUser().getId();
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("userId").is(id)).addCriteria(Criteria.where("checked").is(1));
            List<CartDTO> cartDTOS = mongoTemplate.find(query, CartDTO.class);
            return Result.success(cartDTOS);
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 根据用户id获取购物车
     * @param userId
     * @return
     */
    @Override
    public List<TCart> getUserCheckedCart(Integer userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId)).addCriteria(Criteria.where("checked").is(1));
        return mongoTemplate.find(query, CartDTO.class).stream().map(cartDTO -> {
            TCart tCart = new TCart();
            BeanUtil.copyProperties(cartDTO, tCart);
            return tCart;
        }).collect(Collectors.toList());
    }

    /**
     * 付款后移除用户该订单的购物车信息
     * @param userId
     * @return
     */
    @Override
    public Boolean deleteUserCartAfterPay(Integer userId) {
        String redisKey = USER_CART + userId;
        String redisKeyProduct = USER_CART_PRODUCT + userId;
        LambdaQueryWrapper<TCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TCart::getUserId, userId);
        queryWrapper.eq(TCart::getChecked, 1);
        this.remove(queryWrapper);
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId)).
                addCriteria(Criteria.where("checked").is(1));
        List<CartDTO> allAndRemove = mongoTemplate.findAllAndRemove(query, CartDTO.class);
        for (CartDTO cartDTO : allAndRemove) {
            stringRedisTemplate.opsForSet().remove(redisKey, cartDTO.getId().toString());
            stringRedisTemplate.opsForSet().remove(redisKeyProduct, cartDTO.getProductId().toString());
        }
        return true;
    }
}




