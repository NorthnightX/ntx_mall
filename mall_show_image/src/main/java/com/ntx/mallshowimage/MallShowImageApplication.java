package com.ntx.mallshowimage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, RedisAutoConfiguration.class, MongoAutoConfiguration.class})
public class MallShowImageApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallShowImageApplication.class, args);
        System.out.println("显示图片模块启动成功");
    }
}
