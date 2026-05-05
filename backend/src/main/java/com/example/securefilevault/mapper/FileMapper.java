package com.example.securefilevault.mapper;

import com.example.securefilevault.model.ManagedFile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface FileMapper {

    // アップロード開始時に metadata を登録し、DB 生成の id を取得する。
    @Insert("""
            INSERT INTO files (owner_id, original_filename, content_type, file_size, status)
            VALUES (#{ownerId}, #{originalFilename}, #{contentType}, #{fileSize}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ManagedFile file);

    // 一覧表示では論理削除済みを除外し、ログインユーザーのファイルだけを返す。
    @Select("""
            SELECT id, owner_id, original_filename, content_type, file_size, status, created_at, updated_at
            FROM files
            WHERE owner_id = #{ownerId}
              AND status <> 'DELETED'
            ORDER BY created_at DESC
    """)
    List<ManagedFile> findByOwnerId(Long ownerId);

    // ADMIN ユーザー用に、全ユーザーのファイル metadata を取得する。
    @Select("""
            SELECT id, owner_id, original_filename, content_type, file_size, status, created_at, updated_at
            FROM files
            WHERE status <> 'DELETED'
            ORDER BY created_at DESC
            """)
    List<ManagedFile> findAll();

    // ダウンロード・削除時に fileId で metadata を取得する。
    @Select("""
            SELECT id, owner_id, original_filename, content_type, file_size, status, created_at, updated_at
            FROM files
            WHERE id = #{id}
    """)
    ManagedFile findById(Long id);

    @Select("""
            SELECT COALESCE(SUM(file_size), 0)
            FROM files
            WHERE owner_id = #{ownerId}
              AND status IN ('AVAILABLE', 'UPLOADING')
            """)
    long sumActiveFileSizeByOwnerId(Long ownerId);

    @Select("""
            SELECT COUNT(*)
            FROM files
            WHERE owner_id = #{ownerId}
              AND status IN ('AVAILABLE', 'UPLOADING')
              AND created_at >= CURRENT_DATE()
            """)
    int countTodayActiveUploadsByOwnerId(Long ownerId);

    // S3 処理の結果に応じて UPLOADING / AVAILABLE / FAILED / DELETED を更新する。
    @Update("""
            UPDATE files
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
