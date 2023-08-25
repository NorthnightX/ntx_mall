package com.ntx.mallorder.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.Province;
import com.ntx.mallcommon.domain.Result;

/**
* @author NorthnightX
* @description 针对表【province】的数据库操作Service
* @createDate 2023-08-24 21:19:26
*/
public interface ProvinceService extends IService<Province> {

    Result getProvinces();

    Result getCities(Integer id);

    Result getAreas(String cityId, String provinceId);
}
