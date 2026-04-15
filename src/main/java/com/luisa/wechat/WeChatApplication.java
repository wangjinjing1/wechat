package com.luisa.wechat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.luisa.wechat.chat.persistence")
@EnableScheduling
public class WeChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeChatApplication.class, args);
    }
}
