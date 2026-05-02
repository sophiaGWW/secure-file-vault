package com.example.securefilevault.dto;

import com.example.securefilevault.model.User;

import java.time.LocalDateTime;

public class UserResponse {

    // フロントエンドへ返してよいユーザー情報だけを持つ。passwordHash は含めない。
    private Long id;
    private String username;
    private String role;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        // User entity から公開可能な項目だけを取り出す。
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
