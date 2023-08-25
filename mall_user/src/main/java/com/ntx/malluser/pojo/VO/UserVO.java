package com.ntx.malluser.pojo.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;
@Data
public class UserVO {
    private Long id;
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickName;
    /**
     *
     */
    private String email;

    /**
     *
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

}
