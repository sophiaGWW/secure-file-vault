package com.example.securefilevault.service;

import com.example.securefilevault.dto.FileResponse;
import com.example.securefilevault.mapper.FileMapper;
import com.example.securefilevault.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileListService {

    private final FileMapper fileMapper;

    public FileListService(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    public List<FileResponse> listFiles(User user) {
        return fileMapper.findByOwnerId(user.getId())
                .stream()
                .map(FileResponse::from)
                .toList();
    }
}
