package com.ntx.mallproduct.controller;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TAdvertise;
import com.ntx.mallproduct.service.TAdvertiseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/advertise")
public class AdvertiseController {
    @Autowired
    private TAdvertiseService advertiseService;

    /**
     * 分页查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("/getAdvertise")
    public Result getAdvertise(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                               @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                               @RequestParam Integer status){
        return advertiseService.getAdvertise(pageNum, pageSize, status);
    }

    /**
     * 新建广告
     * @param tAdvertise
     * @return
     */
    @PostMapping("/addAdvertise")
    public Result addAdvertise(@RequestBody TAdvertise tAdvertise){
        return advertiseService.addAdvertise(tAdvertise);
    }

    /**
     * 上下架广告
     * @param tAdvertise
     * @return
     */
    @PutMapping("/updateStatus")
    public Result updateStatus(@RequestBody TAdvertise tAdvertise){
        return advertiseService.updateStatus(tAdvertise);
    }

    /**
     * 获取广告
     * @return
     */
    @GetMapping("/recommend")
    public Result recommend(){
        return advertiseService.recommend();
    }
}
