package com.example.securefilevault.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    // ログイン時に必要な username。空文字は許可しない。
    @NotBlank(message = "username is required")
    private String username;

    // ログイン時に必要な password。空文字は許可しない。
    @NotBlank(message = "password is required")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
