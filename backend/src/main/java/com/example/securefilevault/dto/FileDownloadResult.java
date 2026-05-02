package com.example.securefilevault.dto;

// S3 から取得したファイル本体と、HTTP レスポンスに必要な metadata をまとめる。
public record FileDownloadResult(
        String filename,
        String contentType,
        long contentLength,
        byte[] content
) {
}
