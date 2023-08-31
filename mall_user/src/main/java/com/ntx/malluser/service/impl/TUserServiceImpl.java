package com.ntx.malluser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.malluser.mapper.TUserMapper;
import com.ntx.malluser.pojo.DTO.UserHolder;
import com.ntx.malluser.pojo.ImageVerificationCode;
import com.ntx.malluser.pojo.VO.LoginForm;
import com.ntx.malluser.pojo.VO.UserVO;
import com.ntx.malluser.service.TUserService;
import com.ntx.malluser.utils.JwtUtils;
import com.ntx.malluser.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static com.ntx.malluser.common.SystemConstant.*;

/**
* @author NorthnightX
* @description 针对表【t_user】的数据库操作Service实现
* @createDate 2023-08-21 21:52:04
*/
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser>
    implements TUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getVerification() throws IOException {
        Map<String, String> map = new HashMap<>();
        //获取验证码对象
        ImageVerificationCode imageVerificationCode = new ImageVerificationCode();
        BufferedImage image = imageVerificationCode.getImage();
        String text = imageVerificationCode.getText();
        //生成验证码id
        String str = UUID.randomUUID().toString().replace("-", "");
        String redisKey = LOGIN_CODE + str;
        stringRedisTemplate.opsForValue().set(redisKey, text, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //向网页传输验证码图片
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "JPEG", outputStream);
        byte[] byteArray = outputStream.toByteArray();
        //转成base64格式
        String encode = Base64.getEncoder().encodeToString(byteArray);
        String prefix = "data:image/jpeg;base64,";
        String baseStr = prefix + encode;
        map.put("redisKey", redisKey);
        map.put("base64Str", baseStr);
        System.out.println(text);
        return Result.success(map);
    }

    @Override
    public Result loginAdmin(LoginForm loginForm) {
        String verification = loginForm.getVerification();
        if(verification == null || verification.length() == 0){
            return Result.error("请输入验证码");
        }
        String redisKey = loginForm.getRedisKey();
        String verificationCode = stringRedisTemplate.opsForValue().get(redisKey);
        if(verificationCode == null){
            return Result.error("验证码已过期");
        }
        if(!verificationCode.equalsIgnoreCase(verification)){
            return Result.error("验证码错误");
        }
        //验证码正确
        String userName = loginForm.getUserName();
        if(userName != null){
            LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TUser::getUsername, userName);
            TUser user = this.getOne(queryWrapper);
            //如果是管理员
            if(authorization(user)){
                String MD5Password = MD5.create().digestHex(loginForm.getPassword());
                System.out.println(MD5Password);
                String loginErrorKey = LOGIN_ADMIN_ERROR + user.getUsername();
                String errorCount = stringRedisTemplate.opsForValue().get(loginErrorKey);
                if(errorCount != null){
                    int integer = Integer.parseInt(errorCount);
                    if(integer == 3){
                        Long expire = stringRedisTemplate.getExpire(loginErrorKey);
                        if(expire == null){
                            return Result.error("网络异常，请稍后重试");
                        }
                        return Result.error("您的账号有风险，请" + expire / 60  + "分钟后重试");
                    }
                }
                //如果密码错误
                if(!user.getPassword().equals(MD5Password)){
                    stringRedisTemplate.opsForValue().setIfAbsent(loginErrorKey, String.valueOf(0));
                    Long increment = stringRedisTemplate.opsForValue().increment(loginErrorKey);
                    if(increment == null){
                        return Result.error("网络异常，请稍后再试");
                    }
                    if(increment == 3){
                        stringRedisTemplate.expire(loginErrorKey, LOGIN_ADMIN_ERROR_TTL, TimeUnit.MINUTES);
                    }
                    return Result.error("密码错误, 你还有" + (3 - increment) + "次机会");
                }
                Map<String, String> map = generateToken(user);
                return Result.success(map);

            }
            return Result.error("您没有权限");
        }
        return Result.error("请输入用户名");
    }

    @Override
    public Result loginUser(LoginForm loginForm) {
        String phone = loginForm.getPhone();
        String password = loginForm.getPassword();
        //如果手机格式无效
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.error("请输入正确的手机号");
        }
        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TUser::getPhone, phone);
        TUser user = this.getOne(queryWrapper);
        String MD5Password = MD5.create().digestHex(loginForm.getPassword());
        if(!user.getPassword().equals(MD5Password)){
            return Result.error("密码错误");
        }
        Map<String, String> map = generateToken(user);
        return Result.success(map);
    }

    @Override
    public String getUserName(Integer userId) {
        TUser user = this.getById(userId);
        return user.getUsername();
    }

    @Override
    public Result queryAll(Integer pageNum, Integer pageSize, Integer status, Integer role, String username) {
        Page<TUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(status != null, TUser::getStatus, status);
        queryWrapper.eq(role != null, TUser::getRole, role);
        queryWrapper.eq( TUser::getDeleted, 1);
        queryWrapper.like(username != null && username.length() > 0, TUser::getUsername, username);
        int count = this.count(queryWrapper);
        int offset = (pageNum - 1) * pageSize;
        queryWrapper.last("limit " + offset + "," + pageSize);
        List<TUser> list = this.list(queryWrapper);
        page.setTotal(count);
        page.setRecords(list);
        return Result.success(page);
    }

    @Override
    public Result updateStatus(TUser user) {
        Long id = UserHolder.getUser().getId();
        try {
            if(Objects.equals(id, user.getId())){
                return Result.error("不能修改自己的帐号状态");
            }
            this.update().eq("id", user.getId()).set("status", user.getStatus()).update();
            return Result.success("修改成功");
        } finally {
            UserHolder.removeUser();
        }
    }

    @Override
    public Result delete(int id) {
        Long id1 = UserHolder.getUser().getId();
        try {
            if(id1 == id){
                return Result.error("不能删除自己");
            }
            this.update().eq("id", id).set("deleted", 0).set("status", 0).set("gmt_modified", LocalDateTime.now()).update();
            return Result.success("删除成功");
        } finally {
            UserHolder.removeUser();
        }
    }

    @Override
    public Result updatePassword(Integer id) {
        String password = "123456";
        String digestedHex = MD5.create().digestHex(password);
        this.update().eq("id", id).set("password", digestedHex).set("gmt_modified", LocalDateTime.now()).update();
        return Result.success("重置成功");
    }

    @Override
    public Result updateUser(TUser user) {
        Long id = UserHolder.getUser().getId();
        if(Objects.equals(id, user.getId())){
            TUser tUser = this.getById(id);
            Integer role = tUser.getRole();
            if(!Objects.equals(role, user.getRole())){
                return Result.error("不能自己修改自己的角色");
            }
            if(!Objects.equals(tUser.getStatus(), user.getStatus())){
                return Result.error("不能修改自己的账号状态");
            }
        }
        user.setGmtModified(LocalDateTime.now());
        this.updateById(user);
        return Result.success("修改成功");
    }

    @Override
    public TUser getUser(Integer userId) {
        return this.getById(userId);
    }

    private Map<String, String> generateToken(TUser user){
        String token = JwtUtils.generateToken(JSON.toJSONString(user));
        Map<String, String> map = new HashMap<>();
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        map.put("user", JSON.toJSONString(userVO));
        map.put("token", token);
        return map;
    }

    private Boolean authorization(TUser user){
        //如果是管理员
        if(Objects.equals(user.getRole(), LOGIN_ADMIN)){
            return true;
        }
        return false;
    }

}




