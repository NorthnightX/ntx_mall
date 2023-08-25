package com.ntx.mallvoucher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MallVoucherApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallVoucherApplication.class, args);
        System.out.println("优惠券模块启动成功");
    }
}
