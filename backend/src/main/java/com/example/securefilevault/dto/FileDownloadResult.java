package com.example.securefilevault.dto;

public record FileDownloadResult(
        String filename,
        String contentType,
        long contentLength,
        byte[] content
) {
}
