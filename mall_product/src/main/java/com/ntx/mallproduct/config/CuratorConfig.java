package com.ntx.mallproduct.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CuratorConfig {
    @Bean(initMethod = "start")
    public CuratorFramework curatorFramework(){
        //3秒间隔，重试十次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 10);
        return CuratorFrameworkFactory.builder().
                retryPolicy(retryPolicy).
                namespace("mall").
                connectionTimeoutMs(15 * 1000).
                sessionTimeoutMs(60 * 1000)
                .connectString("8.140.17.235:2181").build();
    }
}
