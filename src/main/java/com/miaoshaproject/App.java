package com.miaoshaproject;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

//@SpringBootApplication(scanBasePackages = {"com.miaoshaproject"})
@SpringBootApplication
@RestController // 加这个就不用加ResponseBody了
@MapperScan(basePackages = "com.miaoshaproject.dao")
public class App {

    @GetMapping("/")
    public String home() {
        return "请访问：'当前网页ip'/resources/index.html";
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
