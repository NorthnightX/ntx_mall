package com.ntx.mallcart.config;

import com.alibaba.fastjson.JSON;
import com.ntx.mallcart.DTO.UserHolder;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.mallcommon.utils.JwtUtils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class UserMsgInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if(token == null || token.length() == 0){
            return true;
        }
        String userFromToken = JwtUtils.getUserFromToken(token);
        TUser user = JSON.parseObject(userFromToken, TUser.class);
        UserHolder.saveUser(user);
        return true;
    }
}
