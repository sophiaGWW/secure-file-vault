package com.example.securefilevault.service;

import com.example.securefilevault.dto.FileResponse;
import com.example.securefilevault.mapper.FileMapper;
import com.example.securefilevault.model.ManagedFile;
import com.example.securefilevault.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileListService {

    // ファイル一覧は DB metadata から取得し、S3 への問い合わせは行わない。
    private final FileMapper fileMapper;

    public FileListService(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    public List<FileResponse> listFiles(User user) {
        // ADMIN は全ユーザーの metadata を参照でき、一般ユーザーは自分のファイルだけを参照する。
        List<ManagedFile> files = isAdmin(user)
                ? fileMapper.findAll()
                : fileMapper.findByOwnerId(user.getId());

        return files
                .stream()
                .map(FileResponse::from)
                .toList();
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }
}
