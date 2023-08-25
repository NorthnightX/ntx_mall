package com.ntx.mallorder.controller;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallorder.service.ProvinceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/province")
public class ProvinceController {

    @Autowired
    private ProvinceService provinceService;

    @GetMapping("getProvinces")
    public Result getProvinces(){
        return provinceService.getProvinces();
    }

    @GetMapping("/getCities/{id}")
    public Result getCities(@PathVariable Integer id){
        return provinceService.getCities(id);
    }

    @GetMapping("/getAreas")
    public Result getAreas(@RequestParam String cityId, String provinceId){
        return provinceService.getAreas(cityId, provinceId);
    }
}
