# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

Secure File Vault 是一个已经上线的权限控制文件管理系统，用于安全地上传、管理、下载和删除 PDF 文件。系统采用前后端分离架构：前端部署在 Netlify，后端部署在 Render，数据库使用 AWS RDS MySQL，文件本体保存到私有 AWS S3 bucket，数据库只保存用户、文件 metadata 和访问审计日志。

在线访问：

```text
https://imaginative-pavlova-90e5f8.netlify.app/
```

## 核心功能

- 用户注册、登录、登出
- JWT 认证与受保护 API 访问
- 登录状态恢复，刷新页面后可通过本地 token 恢复当前用户
- PDF 文件上传，前后端都限制只允许 `application/pdf`
- 最大上传文件大小为 500MB
- 文件列表展示：文件名、所有者 ID、类型、大小、状态、创建时间
- PDF 文件下载，后端从 S3 读取文件后返回给浏览器
- 单个文件删除
- 多选文件后一键批量删除
- 用户级权限控制：普通用户只能访问自己的文件
- 管理员可查看和下载所有未删除文件
- 删除采用逻辑删除：S3 object 会被删除，DB metadata 保留为 `DELETED`
- 上传、下载、删除操作会写入审计日志
- 统一错误响应，前端展示后端返回的错误信息
- Swagger UI / OpenAPI 文档

## 技术栈与平台

Frontend:

- React 19
- Vite 7
- JavaScript
- 原生 CSS
- Browser `fetch`
- JWT 保存于 `localStorage`
- Netlify

Backend:

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Security
- JWT: `io.jsonwebtoken:jjwt`
- BCrypt password hashing
- MyBatis
- MySQL JDBC Driver
- AWS SDK for Java v2: S3
- Springdoc OpenAPI / Swagger UI
- JUnit 5 + Spring MockMvc
- Render

Infrastructure:

- AWS RDS MySQL
- AWS S3 private bucket
- Netlify environment variables
- Render environment variables
- CORS configured for frontend origin

## 系统架构

```text
Browser
  |
  | React + Vite frontend
  v
Netlify
  |
  | HTTPS API requests with Authorization: Bearer <JWT>
  v
Render
  |
  | Spring Boot REST API
  v
AWS RDS MySQL  <---- metadata / users / audit logs
AWS S3         <---- PDF file bodies
```

文件不会以 BLOB 的形式存入数据库。上传时，后端先创建文件 metadata，再使用 DB 生成的 `files.id` 作为 S3 object key 上传文件本体。下载时，后端先检查 DB metadata、文件状态和用户权限，再从 S3 读取文件并返回给浏览器。

## API 一览

公开接口：

| Method | Endpoint | 说明 |
| --- | --- | --- |
| `GET` | `/api/health` | 后端健康检查 |
| `POST` | `/api/auth/register` | 注册用户 |
| `POST` | `/api/auth/login` | 用户登录 |
| `GET` | `/swagger-ui.html` | Swagger UI |

需要 JWT 的接口：

| Method | Endpoint | 说明 |
| --- | --- | --- |
| `GET` | `/api/auth/me` | 获取当前登录用户 |
| `POST` | `/api/files/upload` | 上传 PDF 文件 |
| `GET` | `/api/files` | 获取文件列表 |
| `GET` | `/api/files/{fileId}/download` | 下载文件 |
| `DELETE` | `/api/files/{fileId}` | 删除文件 |

受保护接口需要请求头：

```text
Authorization: Bearer <token>
```

## 权限规则

- 未登录用户只能访问健康检查、注册、登录和 OpenAPI 文档。
- 普通 `USER` 只能查看、下载和删除自己上传的文件。
- `ADMIN` 可以查看和下载所有未删除文件。
- 删除文件只允许文件 owner 操作。
- 状态不是 `AVAILABLE` 的文件不能下载。
- 已删除文件不会作为可用文件继续提供下载。

## 文件生命周期

文件状态：

```text
UPLOADING
AVAILABLE
FAILED
DELETED
```

上传流程：

1. 前端选择 PDF 文件。
2. 前端通过 `multipart/form-data` 调用 `/api/files/upload`。
3. 后端校验文件名、空文件和 Content-Type。
4. 后端向 `files` 表写入 metadata，状态为 `UPLOADING`。
5. 后端使用生成的 `files.id` 作为 S3 object key。
6. 后端调用 AWS SDK 将文件上传到 S3。
7. 上传成功后，文件状态更新为 `AVAILABLE`。
8. 上传失败时，文件状态更新为 `FAILED`。
9. 上传结果写入 `file_access_logs`。

下载流程：

1. 后端根据 `fileId` 查询 metadata。
2. 校验文件存在、状态为 `AVAILABLE`。
3. 校验当前用户是否有权限下载。
4. 使用 `String.valueOf(fileId)` 从 S3 读取文件。
5. 返回 `application/pdf` 响应，并设置原始文件名。
6. 写入下载成功或失败日志。

删除流程：

1. 后端根据 `fileId` 查询 metadata。
2. 校验文件存在且未删除。
3. 校验当前用户是否为 owner。
4. 删除对应 S3 object。
5. DB metadata 状态更新为 `DELETED`。
6. 写入删除成功或失败日志。

## 数据库表

初始化脚本：

```text
backend/src/main/resources/schema.sql
```

主要表：

```text
users
  id
  username
  password_hash
  role
  created_at

files
  id
  owner_id
  original_filename
  content_type
  file_size
  status
  created_at
  updated_at

file_access_logs
  id
  file_id
  user_id
  action
  result
  ip_address
  created_at
```

审计日志中使用的 action / result：

```text
UPLOAD / SUCCESS
UPLOAD / FAILED
DOWNLOAD / SUCCESS
DOWNLOAD / NOT_FOUND
DOWNLOAD / ACCESS_DENIED
DOWNLOAD / FAILED
DELETE / SUCCESS
DELETE / FAILED
```

## 环境变量

Frontend:

```text
VITE_API_BASE_URL=https://your-render-backend-url
```

Backend:

```text
PORT=8080
DB_URL=jdbc:mysql://your-rds-endpoint:3306/secure_file_vault?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=%2B09:00&forceConnectionTimeZoneToSession=true
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password
JWT_SECRET=replace-with-a-random-secret-of-at-least-32-characters
JWT_EXPIRATION_MINUTES=120
CORS_ALLOWED_ORIGINS=https://imaginative-pavlova-90e5f8.netlify.app
AWS_REGION=ap-northeast-1
AWS_S3_BUCKET=your-private-s3-bucket
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
```

本地开发默认值定义在：

```text
backend/src/main/resources/application.yml
frontend/src/api/client.js
```

生产环境中不要使用默认 `JWT_SECRET`，也不要把数据库密码或 AWS access key 写入源码。

## 本地运行

准备数据库：

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```text
http://localhost:8080/api/health
```

启动前端：

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

前端构建：

```bash
cd frontend
npm run build
```

后端测试：

```bash
cd backend
mvn test
```

## 项目结构

```text
secure-file-vault/
  backend/
    Dockerfile
    pom.xml
    src/main/java/com/example/securefilevault/
      config/
      controller/
      dto/
      exception/
      mapper/
      model/
      security/
      service/
      storage/
    src/main/resources/
      application.yml
      schema.sql
    src/test/

  frontend/
    package.json
    index.html
    src/
      api/
      components/
      pages/
      App.jsx
      main.jsx
      styles.css
```

## 部署说明

Frontend on Netlify:

- 构建命令：`npm run build`
- 输出目录：`frontend/dist`
- 需要配置 `VITE_API_BASE_URL` 指向 Render 后端地址

Backend on Render:

- Java 17 / Spring Boot 应用
- 使用 `PORT` 环境变量适配 Render 端口
- 通过环境变量连接 AWS RDS MySQL
- 通过环境变量配置 S3 bucket、AWS region 和 AWS credentials
- 需要将 Netlify 域名加入 `CORS_ALLOWED_ORIGINS`

AWS:

- RDS MySQL 保存用户、文件 metadata、审计日志
- S3 bucket 使用 private 访问策略
- 文件 object key 使用 DB 文件主键字符串
