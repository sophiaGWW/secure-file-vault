package com.example.securefilevault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    // 登録 username は DB の users.username に保存される。
    @NotBlank(message = "username is required")
    @Size(min = 3, max = 100, message = "username must be between 3 and 100 characters")
    private String username;

    // password は Service 層で BCrypt hash に変換して保存する。
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
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
