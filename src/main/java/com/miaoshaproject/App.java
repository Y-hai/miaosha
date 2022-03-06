package com.miaoshaproject;

import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dataobject.UserDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

//@SpringBootApplication(scanBasePackages = {"com.miaoshaproject"})
@SpringBootApplication
@RestController // 加这个就不用加ResponseBody了
@MapperScan(basePackages = "com.miaoshaproject.dao")
public class App {

    @Resource
    private UserDOMapper userDOMapper;

    @GetMapping("/")
    public String home() {
        UserDO userDO = userDOMapper.selectByPrimaryKey(40);
        if (userDO == null) return "用户对象不存在";
        else return userDO.getName();
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
