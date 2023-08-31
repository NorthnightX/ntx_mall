package com.ntx.mallorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TOrderItem;
import com.ntx.mallorder.DTO.UserHolder;
import com.ntx.mallorder.mapper.TOrderItemMapper;
import com.ntx.mallorder.service.TOrderItemService;
import org.springframework.stereotype.Service;

/**
* @author NorthnightX
* @description 针对表【t_order_item】的数据库操作Service实现
* @createDate 2023-08-21 21:55:10
*/
@Service
public class TOrderItemServiceImpl extends ServiceImpl<TOrderItemMapper, TOrderItem>
    implements TOrderItemService {

    @Override
    public Boolean getOrderByProduct(int id) {
        LambdaQueryWrapper<TOrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TOrderItem::getProductId, id);
        queryWrapper.eq(TOrderItem::getDeleted, 1);
        queryWrapper.last("LIMIT 1");
        TOrderItem one = this.getOne(queryWrapper);
        return one == null;
    }

    @Override
    public Boolean isBuyProduct(int userId, int productId) {
        LambdaQueryWrapper<TOrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TOrderItem::getProductId, productId);
        queryWrapper.eq(TOrderItem::getUserId, userId);
        int count = this.count(queryWrapper);
        return count > 0;
    }

    @Override
    public Result isBuy(int productId) {
        Long userId = UserHolder.getUser().getId();
        LambdaQueryWrapper<TOrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TOrderItem::getProductId, productId);
        queryWrapper.eq(TOrderItem::getUserId, userId);
        int count = this.count(queryWrapper);
        if(count > 0){
            return Result.success(true);
        }
        else {
            return Result.success(false);
        }
    }
}




