package com.example.securefilevault.mapper;

import com.example.securefilevault.model.ManagedFile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface FileMapper {

    @Insert("""
            INSERT INTO files (owner_id, original_filename, content_type, file_size, status)
            VALUES (#{ownerId}, #{originalFilename}, #{contentType}, #{fileSize}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ManagedFile file);

    @Select("""
            SELECT id, owner_id, original_filename, content_type, file_size, status, created_at, updated_at
            FROM files
            WHERE owner_id = #{ownerId}
              AND status <> 'DELETED'
            ORDER BY created_at DESC
            """)
    List<ManagedFile> findByOwnerId(Long ownerId);

    @Select("""
            SELECT id, owner_id, original_filename, content_type, file_size, status, created_at, updated_at
            FROM files
            WHERE id = #{id}
            """)
    ManagedFile findById(Long id);

    @Update("""
            UPDATE files
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
