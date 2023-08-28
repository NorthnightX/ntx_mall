package com.ntx.mallproduct.service;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TAdvertise;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author NorthnightX
* @description 针对表【t_advertise】的数据库操作Service
* @createDate 2023-08-27 22:43:20
*/
public interface TAdvertiseService extends IService<TAdvertise> {

    Result getAdvertise(Integer pageNum, Integer pageSize, Integer status);

    Result addAdvertise(TAdvertise tAdvertise);

    Result updateStatus(TAdvertise tAdvertise);

    Result recommend();
}
