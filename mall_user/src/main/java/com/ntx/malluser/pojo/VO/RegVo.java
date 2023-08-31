package com.ntx.malluser.pojo.VO;

import lombok.Data;

@Data
public class RegVo {
    private String username;


    private String nickName;

    private String email;

    private String phone;

    private String password;
    private String confirmPassword;
}
