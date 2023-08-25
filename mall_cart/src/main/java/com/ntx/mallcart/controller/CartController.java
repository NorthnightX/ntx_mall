package com.ntx.mallcart.controller;

import com.baomidou.mybatisplus.extension.api.R;
import com.ntx.mallcart.DTO.CartDTO;
import com.ntx.mallcart.service.TCartService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private TCartService cartService;

    /**
     * 添加购物车
     * @param cart
     * @return
     */
    @PostMapping("/addToCart")
    public Result addToCart(@RequestBody TCart cart){
        return cartService.addToCart(cart);
    }

    /**
     * 获取用户购物车
     * @return
     */
    @GetMapping("/getCart")
    public Result getCart(){
        return cartService.getCart();
    }

    /**
     * 删除用户购物车
     * @param id
     * @return
     */
    @DeleteMapping("/deletedFormCart/{id}")
    public Result deletedFormCart(@PathVariable Integer id){
        return cartService.deletedFormCart(id);
    }

    /**
     * 修改商品选中状态
     * @param cartDTO
     * @return
     */
    @PutMapping("/updateCartCheck")
    public Result updateCartCheck(@RequestBody CartDTO cartDTO){
        return cartService.updateCartCheck(cartDTO);
    }

    /**
     * 更新商品选择数量
     * @param cartDTO
     * @return
     */
    @PutMapping("/updateCartCount")
    public Result updateCartCount(@RequestBody CartDTO cartDTO){
        return cartService.updateCartCount(cartDTO);
    }

    /**
     * 获取需要付款的cart
     * @return
     */
    @GetMapping("/getCartToPay")
    public Result getCartToPay(){
        return cartService.getCartToPay();
    }

    /**
     * 获取用户付款的购物车
     * @param userId
     * @return
     */
    @GetMapping("/getUserCheckedCart")
    public List<TCart> getUserCheckedCart(@RequestParam Integer userId){
        return cartService.getUserCheckedCart(userId);
    }

    /**
     * 付款后删除用户的购物车中的部分信息
     * @param userId
     * @return
     */
    @DeleteMapping("/deleteUserCartAfterPay")
    public Boolean deleteUserCartAfterPay(@RequestParam Integer userId){
        return cartService.deleteUserCartAfterPay(userId);
    }
}
