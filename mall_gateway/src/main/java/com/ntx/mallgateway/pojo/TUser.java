package com.ntx.mallgateway.pojo;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 
 * @TableName t_user
 */
@Data
public class TUser implements Serializable {
    /**
     * 用户表id
     */
    private Long id;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户密码，MD5加密
     */
    private String password;

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

    /**
     * 找回密码问题
     */
    private String question;

    /**
     * 找回密码答案
     */
    private String answer;

    /**
     * 角色0-管理员,1-普通用户
     */
    private Integer role;

    /**
     * 状态（1：正常 0：停用）
     */
    private Integer status;

    /**
     * 逻辑删除 1（true）未删除， 0（false）已删除
     */
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    private LocalDateTime gmtModified;

    private static final long serialVersionUID = 1L;
}