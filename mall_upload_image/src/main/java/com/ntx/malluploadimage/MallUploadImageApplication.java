package com.ntx.malluploadimage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, RedisAutoConfiguration.class, MongoAutoConfiguration.class})
public class MallUploadImageApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallUploadImageApplication.class, args);
        System.out.println("图片上传模块启动成功");
    }
}
