package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.Result;
import com.reggie.entity.User;
import com.reggie.service.UserService;
import com.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送手机短信验证码
     *
     * @param user 用来封装手机号等数据
     * @return 短信是否发送成功信息
     */
    @PostMapping("/sendMsg")
    public Result<String> sendMsg(@RequestBody User user, HttpSession session) {
        log.info("用户手机号：{}", user.getPhone());
        // 获取手机号
        String phone = user.getPhone();
        if (StringUtils.isNotEmpty(phone)) {
            // 生成验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code:{}", code);
            // 调用阿里云短信API
//            SMSUtils.sendMessage("", "", phone, code);
            // 保存生成的验证码(保存到session中)
//            session.setAttribute(phone, code);
            // 将生成的验证码存在redis中,并设置有效时间为5分钟
            redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);
            return Result.success("手机验证码发送成功！");
        }
        return Result.error("短信发送失败！");
    }

    /**
     * 移动端用户登录
     *
     * @param map     用于封装前端传过来的用户手机号和验证码
     * @param session 用于获取session中的验证码
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody Map map, HttpSession session) {
        log.info(map.toString());
        // 获取手机号
        String phone = map.get("phone").toString();
        // 获取验证码
        String code = map.get("code").toString();
        // 从session获取保存的验证码
//        Object phoneCode = session.getAttribute(phone);
        // 从Redis中获取保存的验证码
        Object phoneCode = redisTemplate.opsForValue().get(phone);
        // 进行验证码比对
        if (phoneCode != null && phoneCode.equals(code)) {
            // 成功登录
            LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.eq(User::getPhone, phone);

            User user = userService.getOne(userLambdaQueryWrapper);
            if (user == null) {
                // 判断当前登录用户是否为新用户，是则直接添加到用户表中，自动完成注册（判断手机号是否在user表中）
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user", user.getId());
            redisTemplate.delete(phone);
            return Result.success(user);
        }
        return Result.error("登录失败");
    }
}
