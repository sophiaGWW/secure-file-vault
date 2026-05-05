package com.example.securefilevault.service;

import com.example.securefilevault.exception.BusinessException;
import com.example.securefilevault.mapper.FileAccessLogMapper;
import com.example.securefilevault.mapper.FileMapper;
import com.example.securefilevault.model.ManagedFile;
import com.example.securefilevault.model.User;
import com.example.securefilevault.storage.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadServiceTest {

    @Test
    void uploadFailsWhenUserStorageLimitWouldBeExceeded() {
        FileMapper fileMapper = mock(FileMapper.class);
        S3StorageService s3StorageService = mock(S3StorageService.class);
        FileUploadService fileUploadService = new FileUploadService(
                fileMapper,
                mock(FileAccessLogMapper.class),
                s3StorageService,
                150,
                10
        );

        when(fileMapper.sumActiveFileSizeByOwnerId(1L)).thenReturn(100L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileUploadService.upload(user(), pdfFile(51), "127.0.0.1")
        );

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exception.getStatus());
        verify(fileMapper, never()).insert(any(ManagedFile.class));
        verify(s3StorageService, never()).upload(any(), any(), anyLong(), any());
    }

    @Test
    void uploadFailsWhenDailyUploadLimitIsReached() {
        FileMapper fileMapper = mock(FileMapper.class);
        S3StorageService s3StorageService = mock(S3StorageService.class);
        FileUploadService fileUploadService = new FileUploadService(
                fileMapper,
                mock(FileAccessLogMapper.class),
                s3StorageService,
                150,
                10
        );

        when(fileMapper.sumActiveFileSizeByOwnerId(1L)).thenReturn(0L);
        when(fileMapper.countTodayActiveUploadsByOwnerId(1L)).thenReturn(10);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileUploadService.upload(user(), pdfFile(1), "127.0.0.1")
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
        verify(fileMapper, never()).insert(any(ManagedFile.class));
        verify(s3StorageService, never()).upload(any(), any(), anyLong(), any());
    }

    @Test
    void uploadSucceedsWhenUserIsUnderLimits() {
        FileMapper fileMapper = mock(FileMapper.class);
        FileAccessLogMapper accessLogMapper = mock(FileAccessLogMapper.class);
        S3StorageService s3StorageService = mock(S3StorageService.class);
        FileUploadService fileUploadService = new FileUploadService(
                fileMapper,
                accessLogMapper,
                s3StorageService,
                150,
                10
        );

        when(fileMapper.sumActiveFileSizeByOwnerId(1L)).thenReturn(100L);
        when(fileMapper.countTodayActiveUploadsByOwnerId(1L)).thenReturn(9);
        doAnswer(invocation -> {
            ManagedFile file = invocation.getArgument(0);
            file.setId(42L);
            return null;
        }).when(fileMapper).insert(any(ManagedFile.class));

        ManagedFile uploadedFile = new ManagedFile();
        uploadedFile.setId(42L);
        uploadedFile.setOwnerId(1L);
        uploadedFile.setOriginalFilename("demo.pdf");
        uploadedFile.setContentType(MediaType.APPLICATION_PDF_VALUE);
        uploadedFile.setFileSize(50L);
        uploadedFile.setStatus("AVAILABLE");
        when(fileMapper.findById(42L)).thenReturn(uploadedFile);

        fileUploadService.upload(user(), pdfFile(50), "127.0.0.1");

        verify(fileMapper).insert(any(ManagedFile.class));
        verify(fileMapper).updateStatus(42L, "AVAILABLE");
        verify(s3StorageService).upload(any(), any(), anyLong(), any());
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo-user");
        user.setRole("USER");
        return user;
    }

    private MockMultipartFile pdfFile(int size) {
        return new MockMultipartFile(
                "file",
                "demo.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[size]
        );
    }
}
