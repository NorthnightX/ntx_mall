package com.ntx.malluser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.malluser.config.RabbitMQConfig;
import com.ntx.malluser.mapper.TUserMapper;
import com.ntx.malluser.pojo.DTO.Email;
import com.ntx.malluser.pojo.DTO.UserHolder;
import com.ntx.malluser.pojo.ImageVerificationCode;
import com.ntx.malluser.pojo.VO.LoginForm;
import com.ntx.malluser.pojo.VO.RegVo;
import com.ntx.malluser.pojo.VO.UserVO;
import com.ntx.malluser.service.TUserService;
import com.ntx.malluser.utils.JwtUtils;
import com.ntx.malluser.utils.RegexUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    @Autowired
    private RabbitTemplate rabbitTemplate;

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

    @Override
    public Result getLoginUser() {
        Long id = UserHolder.getUser().getId();
        UserVO userVO = new UserVO();
        TUser user = this.getById(id);
        BeanUtil.copyProperties(user, userVO);
        return Result.success(userVO);
    }

    @Override
    public Result updateUserAvatar(TUser user) {
        String avatar = user.getAvatar();
        Long id = UserHolder.getUser().getId();
        this.update().eq("id", id).set("avatar", avatar).set("gmt_modified", LocalDateTime.now()).update();
        return Result.success("修改成功");
    }

    @Override
    public Result updateUserNickName(TUser user) {
        String nickName = user.getNickName();
        Long id = UserHolder.getUser().getId();
        this.update().eq("id", id).set("nick_name", nickName).set("gmt_modified", LocalDateTime.now()).update();
        return Result.success("修改成功");
    }

    @Override
    public Result updateUserPassword(TUser user) {
        String password = user.getPassword();
        String digested = MD5.create().digestHex(password);
        Long id = UserHolder.getUser().getId();
        this.update().eq("id", id).set("password", digested).set("gmt_modified", LocalDateTime.now()).update();
        return Result.success("修改成功");
    }

    @Override
    public Result updateUserPhone(TUser user) {
        String phone = user.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.error("请输入正确的手机号");
        }
        Long id = UserHolder.getUser().getId();
        this.update().eq("id", id).set("phone", phone).set("gmt_modified", LocalDateTime.now()).update();
        return Result.success("修改成功");
    }

    @Override
    public Result updateUserEmail(TUser user) {
        String email = user.getEmail();
        if(RegexUtils.isEmailInvalid(email)){
            return Result.error("请输入正确的邮箱");
        }
        Long id = UserHolder.getUser().getId();
        this.update().eq("id", id).set("email", email).set("gmt_modified", LocalDateTime.now()).update();
        return Result.success("修改成功");
    }

    @Override
    public Result reg(RegVo regVo) {
        TUser user = new TUser();
        if(!regVo.getConfirmPassword().equals(regVo.getPassword())){
            return Result.error("两次输入的密码不相等");
        }
        BeanUtil.copyProperties(regVo, user);
        String username = user.getUsername();
        if(username == null || username.length() == 0){
            return Result.error("请输入用户名");
        }
        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TUser::getUsername, username);
        TUser isUserNameAbsent = this.getOne(queryWrapper);
        if(isUserNameAbsent != null){
            return Result.error("该用户已存在");
        }
        String phone = user.getPhone();
        LambdaQueryWrapper<TUser> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(TUser::getPhone, phone);
        TUser user1 = this.getOne(queryWrapper1);
        if(user1 != null){
            return Result.error("该手机号已经注册");
        }
        if(phone == null || phone.length() == 0){
            return Result.error("请输入手机号");
        }
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.error("请输入正确的手机号");
        }
        LambdaQueryWrapper<TUser> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(TUser::getEmail, user.getEmail());
        TUser user2 = this.getOne(queryWrapper2);
        if(user2 != null){
            return Result.error("该邮箱已经注册");
        }
        if(RegexUtils.isEmailInvalid(user.getEmail())){
            return Result.error("请输入正确的邮箱");
        }
        String password = user.getPassword();
        if(password == null || password.length() < 8){
            return Result.error("请输入符合格式的密码");
        }
        if(user.getNickName() == null){
            user.setNickName(user.getUsername());
        }
        user.setRole(1);
        user.setGmtModified(LocalDateTime.now());
        user.setGmtCreate(LocalDateTime.now());
        user.setStatus(0);
        user.setDeleted(1);
        user.setAnswer("1");
        user.setQuestion("1");
        user.setDeptId(100L);
        this.save(user);
        long id = user.getId();
        Email email = new Email("激活用户",user.getEmail(),"点击此链接完成验证:http://localhost:10100/user/activeUser/" + id);
        String jsonStr = JSONUtil.toJsonStr(email);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_KEY,jsonStr);
        return Result.success("注册成功");
    }

    @Override
    public Result activeUser(int id) {
        this.update().set("status", 1).eq("id", id).update();
        return Result.success("激活成功");
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




