package com.example.securefilevault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.example.securefilevault.mapper")
@ConfigurationPropertiesScan
public class SecureFileVaultApplication {

    // Spring Boot アプリケーションの起動入口。
    public static void main(String[] args) {
        SpringApplication.run(SecureFileVaultApplication.class, args);
    }
}
