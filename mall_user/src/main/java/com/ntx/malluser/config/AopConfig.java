package com.ntx.malluser.config;

import cn.hutool.jwt.JWTUtil;
import com.alibaba.fastjson.JSON;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.malluser.pojo.DTO.UserHolder;
import com.ntx.malluser.service.TUserService;
import com.ntx.malluser.utils.JwtUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
@Aspect
public class AopConfig {
    @Autowired
    private TUserService userService;


    @After("execution(* com.ntx.malluser.controller.UserController.updateUserAvatar(..)) " +
            "|| execution(* com.ntx.malluser.controller.UserController.updateUserNickName(..)) " +
            "|| execution(* com.ntx.malluser.controller.UserController.updateUserPassword(..)) " +
            "|| execution(* com.ntx.malluser.controller.UserController.updateUserPhone(..))")
    public void afterMethod(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if(arg instanceof HttpServletResponse){
                HttpServletResponse response = (HttpServletResponse) arg;
                Long id = UserHolder.getUser().getId();
                TUser user = userService.getById(id);
                String token = JwtUtils.generateToken(JSON.toJSONString(user));
                response.setHeader("update", token);
            }
        }
        UserHolder.removeUser();
    }
}
