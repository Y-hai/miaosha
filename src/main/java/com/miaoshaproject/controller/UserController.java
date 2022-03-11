package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*") // 跨域
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

//    @Resource
//    private HttpServletRequest httpServletRequest; // 单例模式支持并发访问？

    @Autowired
    private RedisTemplate redisTemplate;

    //用户登录接口
    @ResponseBody
    @PostMapping(value = "/login", consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType login(@RequestParam(name = "telphone") String telphone,
                                  @RequestParam(name = "password") String password)
            throws BusinessException, NoSuchAlgorithmException {
        //入参校验，非空校验
        if (StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户登录服务，用来校验用户登录是否合法，验证成功返回UserModel
        UserModel userModel = userService.validateLogin(telphone, this.EncodeByMd5(password));

        //将登陆凭证加入到用户登录成功的session内
        //修改成若用户登陆成功后将对应的登录信息和登录凭证一起存入redis中

        // 生成登陆凭证token，UUID
        String uuidToken = UUID.randomUUID().toString().replace("-", "");
        //建立token和用户登录态之间的联系
        redisTemplate.opsForValue().set(uuidToken, userModel);
        redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);

//        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
//        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);

        return CommonReturnType.create(uuidToken);
    }

    //用户注册接口
    @ResponseBody
    @PostMapping(value = "/register", consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType register(@RequestParam(name = "telphone") String telphone,
                                     @RequestParam(name = "otpCode") String otpCode,
                                     @RequestParam(name = "name") String name,
                                     @RequestParam(name = "gender") String gender,
                                     @RequestParam(name = "age") String age,
                                     @RequestParam(name = "password") String password)
            throws BusinessException, NoSuchAlgorithmException {
        //验证手机号和对应的otpCode相符合
//        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
        // 获取手机号对应的otp码
        String inSessionOtpCode = (String) redisTemplate.opsForValue().get(telphone);
        // 这个equals主要是有一个判空的处理
        if (!com.alibaba.druid.util.StringUtils.equals(otpCode, inSessionOtpCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码不符合");
        }

        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setAge(Integer.valueOf(age));
        userModel.setGender(Byte.valueOf(gender));
        userModel.setTelphone(telphone);
        userModel.setRegisitMode("byPhone");

        //密码加密
        userModel.setEncrptPassword(this.EncodeByMd5(password));

        userService.register(userModel);
        return CommonReturnType.create(null);
    }

    //密码加密
    public String EncodeByMd5(String str) throws NoSuchAlgorithmException {
        byte[] secretBytes = MessageDigest.getInstance("md5").digest(str.getBytes());
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++)
            md5code = "0" + md5code;
        return md5code;
    }

    //用户获取otp短信接口
    @ResponseBody
    @PostMapping(value = "/getotp", consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType getOtp(@RequestParam(name = "telphone") String telphone) {

        //需要按照一定的规则生成OTP验证码
        Random random = new Random();
        int randomInt = random.nextInt(9999);
        randomInt += 1000;
        String otpCode = String.valueOf(randomInt);

        //将OTP验证码同对应用户的手机号关联，使用httpsession的方式绑定手机号与OTPCDOE
//        httpServletRequest.getSession().setAttribute(telphone, otpCode);
        redisTemplate.opsForValue().set(telphone, otpCode);
        redisTemplate.expire(telphone, 10, TimeUnit.MINUTES);
        //将OTP验证码通过短信通道发送给用户（这里待扩展）
        System.out.println("telphone=" + telphone + "&otpCode=" + otpCode);

        return CommonReturnType.create(otpCode);
    }

    @ResponseBody
    @GetMapping("/get")
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        UserModel userModel = userService.getUserById(id);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        // 有些信息不能传到前端，领域模型转换为视图模型
        UserVO userVO = convertFromModel(userModel);
        // 返回通用对象
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

}
