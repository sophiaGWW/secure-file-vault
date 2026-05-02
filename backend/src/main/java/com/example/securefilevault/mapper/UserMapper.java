package com.example.securefilevault.mapper;

import com.example.securefilevault.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {

    // ログイン時と重複チェック時に username でユーザーを検索する。
    @Select("""
            SELECT id, username, password_hash, role, created_at
            FROM users
            WHERE username = #{username}
    """)
    User findByUsername(String username);

    // JWT の subject に入っている userId から現在ユーザーを復元する。
    @Select("""
            SELECT id, username, password_hash, role, created_at
            FROM users
            WHERE id = #{id}
    """)
    User findById(Long id);

    // 登録時に password_hash を含むユーザー情報を保存する。
    @Insert("""
            INSERT INTO users (username, password_hash, role)
            VALUES (#{username}, #{passwordHash}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
}
