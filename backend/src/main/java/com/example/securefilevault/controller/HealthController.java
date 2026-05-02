package com.example.securefilevault.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        // アプリケーション起動確認用の軽量エンドポイント。
        return "OK";
    }
}
