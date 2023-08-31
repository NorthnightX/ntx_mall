package com.ntx.malluser.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.malluser.pojo.VO.LoginForm;

import java.io.IOException;

/**
* @author NorthnightX
* @description 针对表【t_user】的数据库操作Service
* @createDate 2023-08-21 21:52:04
*/
public interface TUserService extends IService<TUser> {

    Result getVerification() throws IOException;

    Result loginAdmin(LoginForm loginForm);

    Result loginUser(LoginForm loginForm);

    String getUserName(Integer userId);

    Result queryAll(Integer pageNum, Integer pageSize, Integer status, Integer role, String username);

    Result updateStatus(TUser user);

    Result delete(int id);

    Result updatePassword(Integer id);

    Result updateUser(TUser user);

    TUser getUser(Integer userId);

    Result getLoginUser();

    Result updateUserAvatar(TUser user);

    Result updateUserNickName(TUser user);

    Result updateUserPassword(TUser user);

    Result updateUserPhone(TUser user);

    Result updateUserEmail(TUser user);
}
