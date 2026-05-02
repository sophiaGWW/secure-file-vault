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
        ManagedFile file = loadOwnedAvailableFile(user, fileId);
        String s3Key = buildS3Key(file.getId());

        try {
            byte[] content = s3StorageService.download(s3Key);
            writeLog(file.getId(), user.getId(), "DOWNLOAD", "SUCCESS", ipAddress);
            return new FileDownloadResult(
                    file.getOriginalFilename(),
                    MediaType.APPLICATION_PDF_VALUE,
                    content.length,
                    content
            );
        } catch (S3Exception exception) {
            writeLog(file.getId(), user.getId(), "DOWNLOAD", "FAILED", ipAddress);
            if (exception.statusCode() == 404) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "File content not found in S3", exception);
            }
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download file from S3", exception);
        }
    }

    private ManagedFile loadOwnedAvailableFile(User user, Long fileId) {
        ManagedFile file = fileMapper.findById(fileId);
        if (file == null || "DELETED".equals(file.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "File not found");
        }

        if (!file.getOwnerId().equals(user.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have permission to download this file");
        }

        if (!"AVAILABLE".equals(file.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "File is not available for download");
        }

        return file;
    }

    private String buildS3Key(Long fileId) {
        return String.valueOf(fileId);
    }

    private void writeLog(Long fileId, Long userId, String action, String result, String ipAddress) {
        FileAccessLog log = new FileAccessLog();
        log.setFileId(fileId);
        log.setUserId(userId);
        log.setAction(action);
        log.setResult(result);
        log.setIpAddress(ipAddress);
        accessLogMapper.insert(log);
    }
}
