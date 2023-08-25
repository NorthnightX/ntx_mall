package com.ntx.mallproduct.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ESConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient(){
//        return new RestHighLevelClient(RestClient.builder(HttpHost.create("192.168.203.131:9200")));
        return new RestHighLevelClient(RestClient.builder(HttpHost.create("47.113.230.48:9200")));
    }
}
