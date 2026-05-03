# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

这是一个基于 Spring Boot + AWS S3 的权限控制文件管理系统。项目模拟既存系统改修场景：原本 PDF 文件以 DB BLOB 保存，现在迁移为文件本体保存到 AWS S3，DB 只保存 metadata。项目会按阶段实现 JWT 认证、后端 multipart 上传、用户级权限控制、文件元数据 DB 管理和操作日志。

## 第一阶段范围

当前阶段只完成最小项目骨架：

- `backend/`：Java 17 + Spring Boot 3 后端骨架
- `frontend/`：React + Vite 前端骨架
- `GET /api/health` 返回 `OK`
- 前端首页显示 `Secure File Vault`
- 添加 README 初版

当前阶段已实现后端接收 multipart/form-data 后上传到 S3，并提供后端下载和删除接口。

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

生产或共享环境中，请将 `JWT_SECRET` 改成至少 32 字符的随机字符串，不要使用默认值。

## S3 Backend Upload

当前实现模拟既存系统从 DB BLOB 保存迁移到 AWS S3 保存。为了降低既存前端改修范围，前端仍然使用常见的 `multipart/form-data` 上传方式调用后端，文件本体由后端接收后上传到私有 S3 bucket。

后端接口：

```text
POST   /api/files/upload
GET    /api/files
GET    /api/files/{fileId}/download
DELETE /api/files/{fileId}
```

上传流程：

1. 前端选择 PDF 文件。
2. 前端通过 `multipart/form-data` 调用 `POST /api/files/upload`。
3. `FileController` 只负责接收请求和返回响应。
4. `FileUploadService` 校验文件是否为空、Content-Type 是否为 `application/pdf`。
5. 后端先插入 DB metadata，状态为 `UPLOADING`。
6. DB 生成的 `id` 作为 S3 object key。
7. `FileUploadService` 调用 `S3StorageService.upload(...)` 把文件本体保存到 S3。
8. 上传成功后，DB 状态更新为 `AVAILABLE`，并记录操作日志。

下载和删除流程：

- `GET /api/files` 只返回 DB metadata，不返回文件本体。普通用户只能看到 `owner_id` 等于自己 userId 的文件，`ADMIN` 用户可以看到所有未删除文件。
- `FileDownloadService` 根据 `fileId` 查询 DB metadata，校验文件存在、`status = AVAILABLE`，并通过 `owner_id` 做权限校验。
- 普通用户只能下载 `owner_id` 等于自己 userId 的文件，`ADMIN` 可以下载所有文件。
- 权限校验通过后，后端使用 `String.valueOf(fileId)` 作为 S3 object key，调用 `S3StorageService.download(objectKey)` 从 S3 获取文件。
- 下载响应使用 `application/pdf`，并通过 `original_filename` 设置 `Content-Disposition` 文件名。
- 下载成功时记录 `DOWNLOAD / SUCCESS` 日志；文件不存在记录 `DOWNLOAD / NOT_FOUND`；无权限记录 `DOWNLOAD / ACCESS_DENIED`。
- `FileDeleteService` 根据 `fileId` 查询 DB，校验当前用户权限，然后通过 `S3StorageService.delete(s3Key)` 删除 S3 object，并记录删除日志。

允许上传的类型：

```text
application/pdf
```

S3 object key 规则：

```text
objectKey = String.valueOf(fileId)
```

例如 DB 主键是 `123`，则 S3 object key 也是 `123`。

`files` 表只保存 metadata，不保存文件二进制：

```text
id
owner_id
original_filename
content_type
file_size
status
created_at
updated_at
```

`S3StorageService` 独立封装所有 S3 操作，业务 Service 不重复编写 S3 代码：

```text
upload(String s3Key, InputStream inputStream, long contentLength, String contentType)
download(String objectKey)
delete(String s3Key)
exists(String s3Key)
```

Presigned URL 仍然可以作为未来优化方案：当需要降低后端带宽压力时，可以改为后端签发临时 URL，浏览器直接上传到 S3。

AWS 配置来自环境变量或 `application.yml`：

```text
AWS_REGION=ap-northeast-1
AWS_S3_BUCKET=your-private-bucket-name
```

AWS access key 不写在代码里。开发环境建议使用 AWS CLI profile 或环境变量：

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

当前后端上传方式不要求浏览器直接访问 S3，因此本地开发不需要为了上传配置 S3 CORS。

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
- 采用后端上传方式是为了模拟既存系统改修，并降低既存前端改修范围。
- DB 只保存 metadata，文件本体保存到 S3。
- S3 object key 使用 DB 主键，不再额外保存 `s3_key`。
- `S3StorageService` 独立封装上传、下载、删除和存在性检查，便于多个业务 Service 复用。
- 认证密钥和 AWS access key 不写死在代码里，使用环境变量或配置文件。

## 下一阶段建议

下一阶段可以继续优化：

- `GET /api/files/{fileId}/logs`
- 管理员权限和跨用户审计
- Presigned URL 直传，降低后端文件传输带宽
