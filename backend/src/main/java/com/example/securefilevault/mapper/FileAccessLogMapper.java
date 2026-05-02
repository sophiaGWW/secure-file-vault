package com.example.securefilevault.mapper;

import com.example.securefilevault.model.FileAccessLog;
import org.apache.ibatis.annotations.Insert;

public interface FileAccessLogMapper {

    // アップロード・ダウンロード・削除の操作履歴を保存する。
    @Insert("""
            INSERT INTO file_access_logs (file_id, user_id, action, result, ip_address)
            VALUES (#{fileId}, #{userId}, #{action}, #{result}, #{ipAddress})
            """)
    void insert(FileAccessLog log);
}
