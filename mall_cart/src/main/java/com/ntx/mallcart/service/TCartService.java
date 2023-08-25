package com.ntx.mallcart.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcart.DTO.CartDTO;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCart;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_cart】的数据库操作Service
* @createDate 2023-08-21 21:55:47
*/
public interface TCartService extends IService<TCart> {

    Result addToCart(TCart cart);

    Result getCart();

    Result deletedFormCart(Integer id);

    Result updateCartCheck(CartDTO cartDTO);

    Result updateCartCount(CartDTO cartDTO);

    Result getCartToPay();

    List<TCart> getUserCheckedCart(Integer userId);

    Boolean deleteUserCartAfterPay(Integer userId);
}
