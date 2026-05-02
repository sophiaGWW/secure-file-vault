package com.example.securefilevault.storage;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

@Service
public class S3StorageService {

    // S3 操作だけを担当し、業務権限や DB 更新は各 UseCase Service に任せる。
    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public S3StorageService(S3Client s3Client, AwsProperties awsProperties) {
        this.s3Client = s3Client;
        this.awsProperties = awsProperties;
    }

    public void upload(String s3Key, InputStream inputStream, long contentLength, String contentType) {
        // 指定された object key に対して、渡された InputStream をそのまま S3 に保存する。
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket())
                .key(s3Key)
                .contentLength(contentLength)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
    }

    public byte[] download(String s3Key) {
        // S3 object を byte 配列として取得し、Controller から PDF レスポンスとして返す。
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket())
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }

    public void delete(String s3Key) {
        // 指定された object key の S3 object を削除する。
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket())
                .key(s3Key)
                .build();

        s3Client.deleteObject(request);
    }

    public boolean exists(String s3Key) {
        // object 本体を取得せず、HEAD リクエストで存在確認だけを行う。
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket())
                .key(s3Key)
                .build();

        try {
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    private String bucket() {
        // bucket 名は application.yml または環境変数 AWS_S3_BUCKET から取得する。
        return awsProperties.getS3().getBucket();
    }
}
