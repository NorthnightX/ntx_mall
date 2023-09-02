package com.ntx.mallgateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Date;

public class JwtUtils {
    //使用SHA-256进行签名
    private static final String KEY = "ntx_blog_auth";
    private static final Long  TTL = 604800000L;
    private static Key getSigningKey() {
        // 使用 SHA-256 生成密钥
        try {
            byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(keyBytes);
            return new SecretKeySpec(digest, "HmacSHA256");
        } catch (Exception e) {
            throw new RuntimeException("生成密钥出现异常", e);
        }
    }

    //加密
    public static String generateToken(String userInfo) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + TTL);
        return Jwts.builder()
                .setSubject(userInfo)
                //发布时间
                .setIssuedAt(now)
                //过期时间
                .setExpiration(expiration)
                //签名
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                //生成
                .compact();
    }
    //验证token是否有效
    public static boolean validateToken(String token) {
        try {
            //
            Jwts.parserBuilder().
                    //设置解析密钥
                    setSigningKey(getSigningKey()).build().
                    //解析token
                    parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    //获取token的信息
    public static String getUserFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
    //获取token的过期时间
    public static Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }
}