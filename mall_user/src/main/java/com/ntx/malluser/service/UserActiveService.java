package com.ntx.malluser.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.UserActive;

/**
* @author NorthnightX
* @description 针对表【user_active】的数据库操作Service
* @createDate 2023-08-26 09:51:47
*/
public interface UserActiveService extends IService<UserActive> {


    Result history(Integer pageNum, Integer pageSize);

    Result deleteFoot(int id);
}
