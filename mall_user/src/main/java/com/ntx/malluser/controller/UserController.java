package com.ntx.malluser.controller;


import com.ntx.mallcommon.domain.Result;
import com.ntx.malluser.pojo.VO.LoginForm;
import com.ntx.malluser.service.TUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private TUserService userService;

    @GetMapping("/getVerification")
    public Result getVerification() throws IOException {
        return userService.getVerification();

    }

    @PostMapping("/adminLogin")
    public Result mallLogin(@RequestBody LoginForm loginForm){
        return userService.loginAdmin(loginForm);
    }

    @PostMapping("/userLogin")
    public Result userLogin(@RequestBody LoginForm loginForm){
        return userService.loginUser(loginForm);
    }
}
