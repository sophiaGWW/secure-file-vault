package com.example.securefilevault.controller;

import com.example.securefilevault.dto.FileDownloadResult;
import com.example.securefilevault.dto.FileResponse;
import com.example.securefilevault.model.User;
import com.example.securefilevault.service.FileDeleteService;
import com.example.securefilevault.service.FileDownloadService;
import com.example.securefilevault.service.FileListService;
import com.example.securefilevault.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileDownloadService fileDownloadService;
    private final FileDeleteService fileDeleteService;
    private final FileListService fileListService;

    public FileController(
            FileUploadService fileUploadService,
            FileDownloadService fileDownloadService,
            FileDeleteService fileDeleteService,
            FileListService fileListService
    ) {
        this.fileUploadService = fileUploadService;
        this.fileDownloadService = fileDownloadService;
        this.fileDeleteService = fileDeleteService;
        this.fileListService = fileListService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileResponse upload(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest servletRequest
    ) {
        return fileUploadService.upload(user, file, getClientIp(servletRequest));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal User user,
            @PathVariable Long fileId,
            HttpServletRequest servletRequest
    ) {
        FileDownloadResult download = fileDownloadService.download(user, fileId, getClientIp(servletRequest));
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(download.content());
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long fileId,
            HttpServletRequest servletRequest
    ) {
        fileDeleteService.delete(user, fileId, getClientIp(servletRequest));
    }

    @GetMapping
    public List<FileResponse> listFiles(@AuthenticationPrincipal User user) {
        return fileListService.listFiles(user);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
