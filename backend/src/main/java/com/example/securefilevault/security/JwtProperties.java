package com.example.securefilevault.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    // JWT 署名に使う秘密鍵。開発用デフォルトではなく環境変数で上書きする想定。
    private String secret;

    // JWT の有効期限を分単位で管理する。
    private long expirationMinutes;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}
