package com.ntx.mallorder.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TShipping;
import com.ntx.mallorder.DTO.ShippingDTO;

/**
* @author NorthnightX
* @description 针对表【t_shipping】的数据库操作Service
* @createDate 2023-08-21 21:55:15
*/
public interface TShippingService extends IService<TShipping> {

    Result addShipping(TShipping shipping);

    Result getShipping();

    Result deleteShipping(Integer id);

    Result updateShipping(TShipping shipping);
}
