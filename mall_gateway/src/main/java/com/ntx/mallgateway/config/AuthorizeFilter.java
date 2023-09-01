package com.ntx.mallgateway.config;

import com.alibaba.fastjson.JSON;
import com.ntx.mallgateway.pojo.TUser;
import com.ntx.mallgateway.utils.JwtUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {

    
    //不需要拦截的
    private final List<String> allowedUris = Arrays.asList(
            "getVerification", "adminLogin", "query", "image", "userLogin", "queryProductByKeyword","reg","activeUser"
    );
    //需要鉴权的
    private final List<String> needAuthUris = Arrays.asList(
            "updateCategory", "deleteCategory", "addCategory"
    );
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String uri = exchange.getRequest().getURI().toString();
        if (allowedUris.stream().anyMatch(uri::contains)) {
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        boolean needAuth = needAuthUris.stream().anyMatch(uri::contains);
        if (token != null && JwtUtils.validateToken(token)) {
            String userFromToken = JwtUtils.getUserFromToken(token);
            String generateToken = JwtUtils.generateToken(userFromToken);
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().set("Authorization", generateToken);
            if (needAuth) {
                String user = JwtUtils.getUserFromToken(token);
                TUser tUser = JSON.parseObject(user, TUser.class);
                if (tUser != null && tUser.getRole() != null && tUser.getRole() != 0) {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
            }
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
