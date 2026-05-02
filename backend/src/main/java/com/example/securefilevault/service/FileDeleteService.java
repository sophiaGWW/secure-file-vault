package com.example.securefilevault.service;

import com.example.securefilevault.exception.BusinessException;
import com.example.securefilevault.mapper.FileAccessLogMapper;
import com.example.securefilevault.mapper.FileMapper;
import com.example.securefilevault.model.FileAccessLog;
import com.example.securefilevault.model.ManagedFile;
import com.example.securefilevault.model.User;
import com.example.securefilevault.storage.S3StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class FileDeleteService {

    // 削除業務では所有者チェック後に S3 object を削除し、DB 側は論理削除にする。
    private final FileMapper fileMapper;
    private final FileAccessLogMapper accessLogMapper;
    private final S3StorageService s3StorageService;

    public FileDeleteService(
            FileMapper fileMapper,
            FileAccessLogMapper accessLogMapper,
            S3StorageService s3StorageService
    ) {
        this.fileMapper = fileMapper;
        this.accessLogMapper = accessLogMapper;
        this.s3StorageService = s3StorageService;
    }

    public void delete(User user, Long fileId, String ipAddress) {
        ManagedFile file = loadOwnedFile(user, fileId);
        String s3Key = buildS3Key(file.getId());

        try {
            // ファイル本体は S3 から削除し、metadata は監査のため DELETED として残す。
            s3StorageService.delete(s3Key);
            fileMapper.updateStatus(file.getId(), "DELETED");
            writeLog(file.getId(), user.getId(), "DELETE", "SUCCESS", ipAddress);
        } catch (S3Exception exception) {
            // S3 削除失敗時は DB status を変更せず、失敗ログだけを残す。
            writeLog(file.getId(), user.getId(), "DELETE", "FAILED", ipAddress);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file from S3", exception);
        }
    }

    private ManagedFile loadOwnedFile(User user, Long fileId) {
        ManagedFile file = fileMapper.findById(fileId);
        if (file == null || "DELETED".equals(file.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "File not found");
        }

        // 所有者以外は削除不可にする。
        if (!file.getOwnerId().equals(user.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have permission to delete this file");
        }

        return file;
    }

    private String buildS3Key(Long fileId) {
        // DB 主キーを S3 object key として再現する。
        return String.valueOf(fileId);
    }

    private void writeLog(Long fileId, Long userId, String action, String result, String ipAddress) {
        // 削除操作の成功・失敗を監査ログに残す。
        FileAccessLog log = new FileAccessLog();
        log.setFileId(fileId);
        log.setUserId(userId);
        log.setAction(action);
        log.setResult(result);
        log.setIpAddress(ipAddress);
        accessLogMapper.insert(log);
    }
}
