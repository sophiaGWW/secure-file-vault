package com.example.securefilevault.controller;

import com.example.securefilevault.dto.AuthResponse;
import com.example.securefilevault.dto.LoginRequest;
import com.example.securefilevault.dto.RegisterRequest;
import com.example.securefilevault.dto.UserResponse;
import com.example.securefilevault.model.User;
import com.example.securefilevault.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 認証 API の Controller。登録・ログインの実処理は AuthService に委譲する。
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        // 入力値 validation 後、ユーザー登録と JWT 発行を行う。
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        // username/password を検証し、成功時に JWT を返す。
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        // JWT 認証済みユーザーのプロフィールを返す。
        return UserResponse.from(user);
    }
}
