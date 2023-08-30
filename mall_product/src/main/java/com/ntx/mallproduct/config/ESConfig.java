package com.ntx.mallproduct.config;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ESConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient() {
//        return new RestHighLevelClient(RestClient.builder(HttpHost.create("192.168.203.131:9200")));
        return new RestHighLevelClient(
                RestClient.builder(HttpHost.create("47.113.230.48:9200")).
                        setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                            @Override
                            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
                                return requestConfigBuilder.setConnectTimeout(90000000)//25hours
                                        .setSocketTimeout(90000000);
                            }
                        })
                        .setHttpClientConfigCallback(
                                (httpAsyncClientBuilder -> {
                                    httpAsyncClientBuilder.disableAuthCaching();//禁用身份验证缓存
                                    //显式设置keepAliveStrategy
                                    httpAsyncClientBuilder.setKeepAliveStrategy((httpResponse, httpContext) -> TimeUnit.MINUTES.toMillis(3));
                                    //显式开启tcp keepalive
                                    httpAsyncClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build());
                                    return httpAsyncClientBuilder;
                                })
                        )
        );
    }
}
