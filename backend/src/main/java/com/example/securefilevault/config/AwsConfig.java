package com.example.securefilevault.config;

import com.example.securefilevault.storage.AwsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(AwsProperties awsProperties) {
        // DefaultCredentialsProvider により、ローカルでは環境変数、EC2 では IAM Role を自動利用する。
        return S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
