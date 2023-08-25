package com.ntx.malluser.pojo.VO;

import lombok.Data;

@Data
public class LoginForm {
    private String phone;
    private String userName;
    private String password;
    private String verification;
    private String redisKey;

}
