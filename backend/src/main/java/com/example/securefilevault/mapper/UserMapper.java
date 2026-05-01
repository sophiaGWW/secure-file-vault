package com.example.securefilevault.mapper;

import com.example.securefilevault.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {

    @Select("""
            SELECT id, username, password_hash, role, created_at
            FROM users
            WHERE username = #{username}
            """)
    User findByUsername(String username);

    @Select("""
            SELECT id, username, password_hash, role, created_at
            FROM users
            WHERE id = #{id}
            """)
    User findById(Long id);

    @Insert("""
            INSERT INTO users (username, password_hash, role)
            VALUES (#{username}, #{passwordHash}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
}
