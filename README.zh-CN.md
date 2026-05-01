# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

这是一个基于 Spring Boot + AWS S3 的权限控制文件管理系统个人项目。项目会按阶段实现 JWT 认证、S3 Presigned URL、用户级权限控制、文件元数据 DB 管理和操作日志，目标是可以上传到 GitHub，并且适合在日本 IT 面试中讲解。

## 第一阶段范围

当前阶段只完成最小项目骨架：

- `backend/`：Java 17 + Spring Boot 3 后端骨架
- `frontend/`：React + Vite 前端骨架
- `GET /api/health` 返回 `OK`
- 前端首页显示 `Secure File Vault`
- 添加 README 初版

当前第二阶段已实现登录认证。S3 集成和文件上传暂未实现。

## 计划技术栈

Backend:

- Java 17
- Spring Boot 3
- Spring Security + JWT
- MyBatis
- MySQL
- AWS SDK for Java v2
- Swagger / OpenAPI

Frontend:

- React
- Vite
- CSS

## Authentication

第二阶段实现了基于 Spring Security + JWT 的登录认证。

后端接口：

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

认证流程：

1. 用户通过 `POST /api/auth/register` 注册账号。
2. 后端使用 BCrypt 保存密码 hash，不保存明文密码。
3. 注册或登录成功后，后端返回 JWT。
4. 前端将 JWT 保存到 `localStorage`。
5. 前端请求受保护 API 时自动添加请求头：

```text
Authorization: Bearer <token>
```

6. 后端 JWT Filter 校验 token，并从 token 中解析用户身份。
7. 未登录用户不能访问 `GET /api/auth/me` 和后续文件管理页面。

`users` 表：

```text
id
username
password_hash
role
created_at
```

启动后端前，请先确认 MySQL 数据库存在：

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

默认连接配置：

```text
DB_URL=jdbc:mysql://localhost:3306/secure_file_vault?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=dev-only-change-me-secure-file-vault-jwt-secret-please-override
```

面试或正式演示时，请将 `JWT_SECRET` 改成至少 32 字符的随机字符串，不要使用默认值。

## 启动方法

### Backend

```bash
cd backend
mvn spring-boot:run
```

检查接口：

```text
http://localhost:8080/api/health
```

期待结果：

```text
OK
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

打开 Vite 输出的访问地址，首页应显示 `Secure File Vault`。

## 项目结构

```text
secure-file-vault
  backend
    pom.xml
    src/main/java/com/example/securefilevault
      SecureFileVaultApplication.java
      config/OpenApiConfig.java
      controller/HealthController.java
    src/main/resources
      application.yml
    src/test/java/com/example/securefilevault
      SecureFileVaultApplicationTests.java

  frontend
    package.json
    index.html
    src
      App.jsx
      main.jsx
      styles.css
```

## 设计思路

- 先分离后端和前端目录，方便后续按阶段扩展。
- S3 bucket 按 private 设计，用户不能直接访问 public URL。
- 上传和下载会在后续阶段通过后端生成 Presigned URL 完成。
- 认证密钥和 AWS access key 不写死在代码里，使用环境变量或配置文件。

## 下一阶段建议

下一阶段实现文件上传入口和文件元数据表：

- `files` 表
- 文件状态 `UPLOADING / AVAILABLE / DELETED / FAILED`
- 生成 S3 Presigned Upload URL
- 上传完成后通知后端
- 文件列表只展示当前用户的文件
