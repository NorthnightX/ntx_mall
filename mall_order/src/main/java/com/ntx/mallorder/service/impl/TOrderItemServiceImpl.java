package com.ntx.mallorder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.mallcommon.domain.TOrderItem;
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

}




