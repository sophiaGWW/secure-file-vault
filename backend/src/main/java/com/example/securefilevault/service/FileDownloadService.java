package com.example.securefilevault.service;

import com.example.securefilevault.dto.FileDownloadResult;
import com.example.securefilevault.exception.BusinessException;
import com.example.securefilevault.mapper.FileAccessLogMapper;
import com.example.securefilevault.mapper.FileMapper;
import com.example.securefilevault.model.FileAccessLog;
import com.example.securefilevault.model.ManagedFile;
import com.example.securefilevault.model.User;
import com.example.securefilevault.storage.S3StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class FileDownloadService {

    // ダウンロード業務では所有者チェック後に S3 からファイル本体を取得する。
    private final FileMapper fileMapper;
    private final FileAccessLogMapper accessLogMapper;
    private final S3StorageService s3StorageService;

    public FileDownloadService(
            FileMapper fileMapper,
            FileAccessLogMapper accessLogMapper,
            S3StorageService s3StorageService
    ) {
        this.fileMapper = fileMapper;
        this.accessLogMapper = accessLogMapper;
        this.s3StorageService = s3StorageService;
    }

    public FileDownloadResult download(User user, Long fileId, String ipAddress) {
        ManagedFile file = loadDownloadableFile(user, fileId, ipAddress);
        String objectKey = buildObjectKey(file.getId());

        try {
            // 他業務でも再利用できるよう、S3 取得処理は共通 Service に委譲する。
            byte[] content = s3StorageService.download(objectKey);
            writeLog(file.getId(), user.getId(), "DOWNLOAD", "SUCCESS", ipAddress);
            return new FileDownloadResult(
                    file.getOriginalFilename(),
                    MediaType.APPLICATION_PDF_VALUE,
                    content.length,
                    content
            );
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                // DB metadata があっても S3 object が存在しない場合は 404 として扱う。
                writeLog(file.getId(), user.getId(), "DOWNLOAD", "NOT_FOUND", ipAddress);
                throw new BusinessException(HttpStatus.NOT_FOUND, "File content not found in S3", exception);
            }
            writeLog(file.getId(), user.getId(), "DOWNLOAD", "FAILED", ipAddress);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download file from S3", exception);
        }
    }

    private ManagedFile loadDownloadableFile(User user, Long fileId, String ipAddress) {
        ManagedFile file = fileMapper.findById(fileId);
        if (file == null) {
            writeLog(null, user.getId(), "DOWNLOAD", "NOT_FOUND", ipAddress);
            throw new BusinessException(HttpStatus.NOT_FOUND, "File not found");
        }

        // ADMIN は全ユーザーのファイルをダウンロードでき、一般ユーザーは owner_id が自分のものだけを許可する。
        if (!canDownload(user, file)) {
            writeLog(file.getId(), user.getId(), "DOWNLOAD", "ACCESS_DENIED", ipAddress);
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have permission to download this file");
        }

        // アップロード失敗・処理中のファイルは返さない。
        if (!"AVAILABLE".equals(file.getStatus())) {
            writeLog(file.getId(), user.getId(), "DOWNLOAD", "FAILED", ipAddress);
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "File is not available for download. Current status: " + file.getStatus());
        }

        return file;
    }

    private boolean canDownload(User user, ManagedFile file) {
        return "ADMIN".equals(user.getRole()) || file.getOwnerId().equals(user.getId());
    }

    private String buildObjectKey(Long fileId) {
        // アップロード時と同じく、DB 主キーを S3 object key とする。
        return String.valueOf(fileId);
    }

    private void writeLog(Long fileId, Long userId, String action, String result, String ipAddress) {
        // ダウンロード成功・失敗を監査ログとして保存する。
        FileAccessLog log = new FileAccessLog();
        log.setFileId(fileId);
        log.setUserId(userId);
        log.setAction(action);
        log.setResult(result);
        log.setIpAddress(ipAddress);
        accessLogMapper.insert(log);
    }
}
