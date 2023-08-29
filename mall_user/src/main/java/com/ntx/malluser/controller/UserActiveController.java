package com.ntx.malluser.controller;

import cn.hutool.core.date.DateTime;
import com.ntx.mallcommon.domain.Result;
import com.ntx.malluser.service.UserActiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/active")
public class UserActiveController {
    @Autowired
    private UserActiveService userActiveService;

    @GetMapping("/history")
    public Result userHistory(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                              @RequestParam(required = false, defaultValue = "10") Integer pageSize){
        return userActiveService.history(pageNum, pageSize);
    }

    @DeleteMapping("/deleteFoot")
    public Result deleteFoot(@RequestParam int id){
        return userActiveService.deleteFoot(id);
    }
}
