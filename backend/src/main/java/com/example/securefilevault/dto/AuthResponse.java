package com.example.securefilevault.dto;

public class AuthResponse {

    // ログイン・登録成功時に、JWT と画面表示用ユーザー情報をまとめて返す DTO。
    private String token;
    private UserResponse user;

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
