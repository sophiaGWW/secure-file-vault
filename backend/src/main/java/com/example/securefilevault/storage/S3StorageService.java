package com.example.securefilevault.storage;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class S3StorageService {

    // S3 Multipart Upload では、最後の part 以外は 5MB 以上である必要がある。
    private static final long MIN_MULTIPART_PART_SIZE_BYTES = 5L * 1024 * 1024;

    // S3 操作だけを担当し、業務権限や DB 更新は各 UseCase Service に任せる。
    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public S3StorageService(S3Client s3Client, AwsProperties awsProperties) {
        this.s3Client = s3Client;
        this.awsProperties = awsProperties;
    }

    public void upload(String s3Key, InputStream inputStream, long contentLength, String contentType) {
        // 大きなファイルは単一 putObject ではなく、S3 側の Multipart Upload API に切り替える。
        if (contentLength > multipartUploadThresholdBytes()) {
            uploadMultipart(s3Key, inputStream, contentLength, contentType);
            return;
        }

        // 指定された object key に対して、渡された InputStream をそのまま S3 に保存する。
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket())
                .key(s3Key)
                .contentLength(contentLength)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
    }

    private void uploadMultipart(String s3Key, InputStream inputStream, long contentLength, String contentType) {
        // まず multipart upload を開始し、返された uploadId を後続の part upload に使う。
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket())
                .key(s3Key)
                .contentType(contentType)
                .build();
        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();
        List<CompletedPart> completedParts = new ArrayList<>();

        try {
            long remainingBytes = contentLength;
            int partNumber = 1;
            int partSize = multipartPartSizeBytes();

            // 入力ストリーム全体をメモリに載せず、設定された part サイズごとに読み込んで S3 へ送る。
            while (remainingBytes > 0) {
                int bytesToRead = (int) Math.min(partSize, remainingBytes);
                byte[] partBytes = readPart(inputStream, bytesToRead);
                remainingBytes -= partBytes.length;

                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucket())
                        .key(s3Key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength((long) partBytes.length)
                        .build();
                UploadPartResponse uploadPartResponse = s3Client.uploadPart(
                        uploadPartRequest,
                        RequestBody.fromBytes(partBytes)
                );

                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build());
                partNumber++;
            }

            // 全 part の ETag を S3 に渡して、1 つの object として確定させる。
            CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucket())
                    .key(s3Key)
                    .uploadId(uploadId)
                    .multipartUpload(completedUpload)
                    .build();
            s3Client.completeMultipartUpload(completeRequest);
        } catch (Exception exception) {
            // 途中で失敗した multipart upload は S3 側に残さないよう abort してから呼び出し元へ失敗を返す。
            abortMultipartUpload(s3Key, uploadId, exception);
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to upload file to S3 with multipart upload", exception);
        }
    }

    private byte[] readPart(InputStream inputStream, int bytesToRead) throws IOException {
        // InputStream#read は指定バイト数未満で返ることがあるため、part サイズまで繰り返し読む。
        byte[] buffer = new byte[bytesToRead];
        int offset = 0;

        while (offset < bytesToRead) {
            int read = inputStream.read(buffer, offset, bytesToRead - offset);
            if (read == -1) {
                break;
            }
            offset += read;
        }

        if (offset == 0) {
            throw new IOException("Unexpected end of upload stream");
        }

        if (offset == bytesToRead) {
            return buffer;
        }

        return Arrays.copyOf(buffer, offset);
    }

    private void abortMultipartUpload(String s3Key, String uploadId, Exception originalException) {
        // abort 自体の失敗は元の例外に suppressed として残し、主原因を失わないようにする。
        try {
            AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                    .bucket(bucket())
                    .key(s3Key)
                    .uploadId(uploadId)
                    .build();
            s3Client.abortMultipartUpload(abortRequest);
        } catch (Exception abortException) {
            originalException.addSuppressed(abortException);
        }
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

    private long multipartUploadThresholdBytes() {
        // application.yml の DataSize 設定を byte 単位の比較値に変換する。
        return awsProperties.getS3().getMultipartUploadThreshold().toBytes();
    }

    private int multipartPartSizeBytes() {
        // 誤って 5MB 未満に設定されても、S3 の multipart 制約を満たす値へ補正する。
        long configuredPartSize = awsProperties.getS3().getMultipartPartSize().toBytes();
        long effectivePartSize = Math.max(configuredPartSize, MIN_MULTIPART_PART_SIZE_BYTES);
        if (effectivePartSize > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) effectivePartSize;
    }
}
