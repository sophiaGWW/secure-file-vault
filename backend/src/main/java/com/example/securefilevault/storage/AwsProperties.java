package com.example.securefilevault.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    // AWS リージョン。例: ap-northeast-1。
    private String region;

    // S3 bucket 関連設定。
    private S3 s3 = new S3();

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public static class S3 {
        // ファイル本体を保存する private bucket 名。
        private String bucket;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }
}
