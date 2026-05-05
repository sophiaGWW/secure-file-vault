package com.example.securefilevault.storage;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

class S3StorageServiceTest {

    // S3 Multipart Upload の閾値検証で使う byte 単位の定数。
    private static final long ONE_MIB = 1024L * 1024;
    private static final long FIVE_MIB = 5 * ONE_MIB;

    @Test
    void uploadUsesPutObjectWhenContentLengthIsAtThreshold() {
        // 閾値ちょうどのファイルは単一 putObject のまま送信する。
        S3Client s3Client = mock(S3Client.class);
        S3StorageService storageService = new S3StorageService(s3Client, properties(100 * ONE_MIB, 16 * ONE_MIB));

        storageService.upload("42", new ByteArrayInputStream(new byte[0]), 100 * ONE_MIB, MediaType.APPLICATION_PDF_VALUE);

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putRequestCaptor.capture(), any(RequestBody.class));
        verify(s3Client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));

        PutObjectRequest putRequest = putRequestCaptor.getValue();
        assertEquals("secure-file-vault-test", putRequest.bucket());
        assertEquals("42", putRequest.key());
        assertEquals(100 * ONE_MIB, putRequest.contentLength());
        assertEquals(MediaType.APPLICATION_PDF_VALUE, putRequest.contentType());
    }

    @Test
    void uploadUsesMultipartUploadWhenContentLengthIsAboveThreshold() {
        // 閾値を超えたファイルは part ごとに分割して Multipart Upload を完了する。
        S3Client s3Client = mock(S3Client.class);
        S3StorageService storageService = new S3StorageService(s3Client, properties(FIVE_MIB, FIVE_MIB));
        long contentLength = 12 * ONE_MIB + 123;

        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-1").build());
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenReturn(
                        UploadPartResponse.builder().eTag("etag-1").build(),
                        UploadPartResponse.builder().eTag("etag-2").build(),
                        UploadPartResponse.builder().eTag("etag-3").build()
                );

        storageService.upload("99", fixedSizeStream(contentLength), contentLength, MediaType.APPLICATION_PDF_VALUE);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));

        ArgumentCaptor<UploadPartRequest> uploadPartCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
        verify(s3Client, times(3)).uploadPart(uploadPartCaptor.capture(), any(RequestBody.class));
        List<UploadPartRequest> uploadPartRequests = uploadPartCaptor.getAllValues();
        assertEquals(List.of(1, 2, 3), uploadPartRequests.stream().map(UploadPartRequest::partNumber).toList());
        assertEquals(FIVE_MIB, uploadPartRequests.get(0).contentLength());
        assertEquals(FIVE_MIB, uploadPartRequests.get(1).contentLength());
        assertEquals(2 * ONE_MIB + 123, uploadPartRequests.get(2).contentLength());

        ArgumentCaptor<CompleteMultipartUploadRequest> completeCaptor =
                ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(s3Client).completeMultipartUpload(completeCaptor.capture());
        CompleteMultipartUploadRequest completeRequest = completeCaptor.getValue();
        assertEquals("secure-file-vault-test", completeRequest.bucket());
        assertEquals("99", completeRequest.key());
        assertEquals("upload-1", completeRequest.uploadId());
        assertEquals(List.of("etag-1", "etag-2", "etag-3"),
                completeRequest.multipartUpload().parts().stream().map(part -> part.eTag()).toList());
    }

    @Test
    void uploadAbortsMultipartUploadWhenPartUploadFails() {
        // part upload の途中失敗時は未完了の multipart upload を abort する。
        S3Client s3Client = mock(S3Client.class);
        S3StorageService storageService = new S3StorageService(s3Client, properties(FIVE_MIB, FIVE_MIB));
        long contentLength = 10 * ONE_MIB;

        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-2").build());
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenReturn(UploadPartResponse.builder().eTag("etag-1").build())
                .thenThrow(S3Exception.builder().message("part failed").build());

        assertThrows(S3Exception.class,
                () -> storageService.upload("100", fixedSizeStream(contentLength), contentLength, MediaType.APPLICATION_PDF_VALUE));

        verify(s3Client, times(2)).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
        ArgumentCaptor<AbortMultipartUploadRequest> abortCaptor =
                ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(s3Client).abortMultipartUpload(abortCaptor.capture());
        verify(s3Client, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

        AbortMultipartUploadRequest abortRequest = abortCaptor.getValue();
        assertEquals("secure-file-vault-test", abortRequest.bucket());
        assertEquals("100", abortRequest.key());
        assertEquals("upload-2", abortRequest.uploadId());
    }

    private static AwsProperties properties(long thresholdBytes, long partSizeBytes) {
        // 実 AWS に依存しないよう、テスト用 bucket とサイズ設定だけを組み立てる。
        AwsProperties properties = new AwsProperties();
        AwsProperties.S3 s3 = new AwsProperties.S3();
        s3.setBucket("secure-file-vault-test");
        s3.setMultipartUploadThreshold(DataSize.ofBytes(thresholdBytes));
        s3.setMultipartPartSize(DataSize.ofBytes(partSizeBytes));
        properties.setS3(s3);
        return properties;
    }

    private static InputStream fixedSizeStream(long size) {
        // 大容量 byte 配列を作らず、指定サイズ分だけ 0 を返す InputStream を用意する。
        return new InputStream() {
            private long remaining = size;

            @Override
            public int read() {
                if (remaining <= 0) {
                    return -1;
                }
                remaining--;
                return 0;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) {
                if (remaining <= 0) {
                    return -1;
                }
                int bytesToRead = (int) Math.min(length, remaining);
                Arrays.fill(bytes, offset, offset + bytesToRead, (byte) 0);
                remaining -= bytesToRead;
                return bytesToRead;
            }
        };
    }
}
