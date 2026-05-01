package com.example.securefilevault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.example.securefilevault.mapper")
public class SecureFileVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureFileVaultApplication.class, args);
    }
}
