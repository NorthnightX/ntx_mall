package com.ntx.malluser.controller;


import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.malluser.pojo.DTO.UserHolder;
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

    @GetMapping("/getUserName")
    public String getUserName(@RequestParam Integer userId){
        return userService.getUserName(userId);
    }

    @GetMapping("/queryAll")
    public Result queryAll(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                           @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) Integer role,
                           @RequestParam(required = false) String username){
        return userService.queryAll(pageNum, pageSize, status, role, username);
    }

    @PutMapping("/updateStatus")
    public Result updateStatus(@RequestBody TUser user){
        return userService.updateStatus(user);
    }

    @DeleteMapping("/delete")
    public Result delete(@RequestParam int id){
        return userService.delete(id);
    }

    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestParam Integer id){
        return userService.updatePassword(id);
    }

    @PutMapping("/updateUser")
    public Result updateUser(@RequestBody TUser user){
        return userService.updateUser(user);
    }
}
