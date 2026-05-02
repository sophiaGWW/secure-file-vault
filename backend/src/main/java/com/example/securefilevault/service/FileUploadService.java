package com.example.securefilevault.service;

import com.example.securefilevault.dto.FileResponse;
import com.example.securefilevault.exception.BusinessException;
import com.example.securefilevault.mapper.FileAccessLogMapper;
import com.example.securefilevault.mapper.FileMapper;
import com.example.securefilevault.model.FileAccessLog;
import com.example.securefilevault.model.ManagedFile;
import com.example.securefilevault.model.User;
import com.example.securefilevault.storage.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final FileMapper fileMapper;
    private final FileAccessLogMapper accessLogMapper;
    private final S3StorageService s3StorageService;

    public FileUploadService(
            FileMapper fileMapper,
            FileAccessLogMapper accessLogMapper,
            S3StorageService s3StorageService
    ) {
        this.fileMapper = fileMapper;
        this.accessLogMapper = accessLogMapper;
        this.s3StorageService = s3StorageService;
    }

    public FileResponse upload(User user, MultipartFile multipartFile, String ipAddress) {
        validateUploadFile(multipartFile);

        ManagedFile file = new ManagedFile();
        file.setOwnerId(user.getId());
        file.setOriginalFilename(multipartFile.getOriginalFilename().trim());
        file.setContentType(MediaType.APPLICATION_PDF_VALUE);
        file.setFileSize(multipartFile.getSize());
        file.setStatus("UPLOADING");
        fileMapper.insert(file);

        String s3Key = buildS3Key(file.getId());

        try (InputStream inputStream = multipartFile.getInputStream()) {
            s3StorageService.upload(s3Key, inputStream, multipartFile.getSize(), MediaType.APPLICATION_PDF_VALUE);
        } catch (Exception exception) {
            log.error("Failed to upload file to S3. fileId={}, s3Key={}, filename={}",
                    file.getId(), s3Key, file.getOriginalFilename(), exception);
            fileMapper.updateStatus(file.getId(), "FAILED");
            writeLog(file.getId(), user.getId(), "UPLOAD", "FAILED", ipAddress);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to S3", exception);
        }

        fileMapper.updateStatus(file.getId(), "AVAILABLE");
        writeLog(file.getId(), user.getId(), "UPLOAD", "SUCCESS", ipAddress);

        ManagedFile uploadedFile = fileMapper.findById(file.getId());
        return FileResponse.from(uploadedFile);
    }

    private void validateUploadFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String filename = multipartFile.getOriginalFilename() == null
                ? ""
                : multipartFile.getOriginalFilename().trim();
        if (filename.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Filename is required");
        }

        if (!MediaType.APPLICATION_PDF_VALUE.equals(multipartFile.getContentType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only PDF files are allowed");
        }
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
