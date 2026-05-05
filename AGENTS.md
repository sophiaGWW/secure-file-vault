# Secure File Vault 项目说明

## 项目概览

这是一个基于 Spring Boot + React 的权限控制文件管理系统，目标场景是把原本保存在数据库 BLOB 中的 PDF 文件迁移到 AWS S3：文件本体存放在私有 S3 bucket，数据库只保存 metadata 和访问日志。

项目目前是个人开发项目，核心功能包括：

- JWT 登录认证
- 后端接收 `multipart/form-data` 文件上传
- PDF 文件保存到 AWS S3
- 用户级权限控制
- 文件 metadata 的 MySQL 管理
- 上传、下载、删除操作日志

## 目录结构

```text
secure-file-vault/
  backend/               Spring Boot 后端
  frontend/              React + Vite 前端
  .github/               GitHub 相关配置
  .vscode/               VS Code 配置
  README.md              日文 README，但当前终端输出存在编码显示问题
  README.zh-CN.md        中文 README，但当前终端输出存在编码显示问题
  README.en.md           英文 README
  AGENTS.md              给后续代理/维护者阅读的项目笔记
```

## 技术栈

### 后端

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Security
- JWT：`io.jsonwebtoken:jjwt`
- MyBatis
- MySQL
- AWS SDK for Java v2，主要使用 S3
- Springdoc OpenAPI / Swagger UI
- JUnit 5 + Spring MockMvc

### 前端

- React 19
- Vite 7
- 原生 CSS
- 浏览器 `fetch`
- JWT 保存在 `localStorage`

## 后端模块说明

后端主包名是 `com.example.securefilevault`。

核心目录：

- `controller/`：REST API 入口。
- `service/`：业务流程，如认证、上传、下载、删除、列表。
- `mapper/`：MyBatis 注解式 SQL Mapper。
- `model/`：数据库实体模型。
- `dto/`：API 请求/响应对象。
- `security/`：JWT 生成、校验、认证过滤器。
- `storage/`：AWS S3 封装和 AWS 配置属性。
- `config/`：Spring Security、AWS、OpenAPI 配置。
- `exception/`：业务异常和统一异常响应。

主要 API：

```text
GET    /api/health
POST   /api/auth/register
POST   /api/auth/login
GET    /api/auth/me
POST   /api/files/upload
GET    /api/files
GET    /api/files/{fileId}/download
DELETE /api/files/{fileId}
```

Swagger UI 路径：

```text
http://localhost:8080/swagger-ui.html
```

## 数据库设计

数据库初始化脚本在：

```text
backend/src/main/resources/schema.sql
```

应用启动时通过 `spring.sql.init.mode=always` 执行 schema 初始化。

数据库表：

- `users`：用户账号、密码 hash、角色。
- `files`：文件 metadata，不保存文件二进制。
- `file_access_logs`：文件访问审计日志。

重要字段：

```text
users:
  id
  username
  password_hash
  role
  created_at

files:
  id
  owner_id
  original_filename
  content_type
  file_size
  status
  created_at
  updated_at

file_access_logs:
  id
  file_id
  user_id
  action
  result
  ip_address
  created_at
```

文件状态约定：

```text
UPLOADING
AVAILABLE
FAILED
DELETED
```

日志动作/结果中已使用的值包括：

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

## 认证与权限

认证逻辑在：

- `AuthController`
- `AuthService`
- `JwtService`
- `JwtAuthenticationFilter`
- `SecurityConfig`

认证流程：

1. 用户通过 `/api/auth/register` 注册，后端使用 BCrypt 保存密码 hash。
2. 注册或登录成功后，后端返回 JWT 和用户信息。
3. 前端将 JWT 保存到 `localStorage` 的 `secure_file_vault_token`。
4. 后续请求通过 `Authorization: Bearer <token>` 访问受保护 API。
5. 后端 JWT Filter 解析 token subject 中的 userId，并从 DB 查询当前用户。

权限规则：

- `GET /api/health`、注册、登录、Swagger/OpenAPI 公开。
- 其他接口都需要认证。
- 普通 `USER` 只能查看和下载自己的文件。
- `ADMIN` 可以查看和下载所有未删除文件。
- 删除文件当前只允许文件 owner 操作，`ADMIN` 没有额外删除特权。

## 文件上传、下载、删除流程

上传入口：

```text
POST /api/files/upload
```

上传实现：

- 前端使用 `multipart/form-data`，字段名是 `file`。
- 后端只允许 `Content-Type = application/pdf`。
- 后端先插入 `files` metadata，状态为 `UPLOADING`。
- DB 生成的 `files.id` 直接作为 S3 object key。
- 文件本体通过 `S3StorageService.upload(...)` 上传到 S3。
- 上传成功后状态改为 `AVAILABLE`，失败时改为 `FAILED`。

下载实现：

- 根据 `fileId` 查询 metadata。
- 校验文件存在、状态为 `AVAILABLE`、用户有权限。
- 使用 `String.valueOf(fileId)` 作为 S3 object key 下载。
- Controller 返回 `application/pdf`，并用原文件名设置 `Content-Disposition`。

删除实现：

- 根据 `fileId` 查询 metadata。
- 校验文件存在且未 `DELETED`。
- 当前只允许 owner 删除。
- 删除 S3 object 后，把 DB 状态更新为 `DELETED`，不是物理删除 metadata。

S3 封装在：

```text
backend/src/main/java/com/example/securefilevault/storage/S3StorageService.java
```

提供方法：

```text
upload(String s3Key, InputStream inputStream, long contentLength, String contentType)
download(String s3Key)
delete(String s3Key)
exists(String s3Key)
```

## 配置说明

后端配置文件：

```text
backend/src/main/resources/application.yml
```

默认数据库配置：

```text
DB_URL=jdbc:mysql://localhost:3306/secure_file_vault?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=%2B09:00&forceConnectionTimeZoneToSession=true
DB_USERNAME=root
DB_PASSWORD=password
```

启动前需要有数据库：

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

JWT 配置：

```text
JWT_SECRET=dev-only-change-me-secure-file-vault-jwt-secret-please-override
JWT_EXPIRATION_MINUTES=120
```

注意：正式演示或部署时必须把 `JWT_SECRET` 换成至少 32 字符以上的随机字符串。

AWS 配置：

```text
aws.region=ap-northeast-1
aws.s3.bucket=secure-file-vault-dev-sophia-20260502
```

`S3Client` 使用 AWS SDK 的 `DefaultCredentialsProvider`。本地开发可用 AWS CLI profile 或环境变量：

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

前端 API base URL：

```text
VITE_API_BASE_URL=http://localhost:8080
```

如果未设置，默认请求 `http://localhost:8080`。

后端 CORS 当前只允许：

```text
http://localhost:5173
```

## 启动与验证

后端：

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```text
http://localhost:8080/api/health
```

期望返回：

```text
OK
```

前端：

```bash
cd frontend
npm install
npm run dev
```

Vite 默认端口通常是：

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

当前测试主要覆盖 `/api/health`，使用 `MockMvc` 单测 `HealthController`，没有启动完整 Spring Context。

## 前端实现要点

入口：

- `frontend/src/main.jsx`
- `frontend/src/App.jsx`

API 封装：

- `frontend/src/api/client.js`
- `frontend/src/api/authApi.js`
- `frontend/src/api/fileApi.js`

页面与组件：

- `LoginPage.jsx`
- `RegisterPage.jsx`
- `FileDashboard.jsx`
- `FileUpload.jsx`
- `FileTable.jsx`

前端状态流：

- `App.jsx` 管理登录、注册、登出、当前用户、当前页面。
- 启动时如果 `localStorage` 有 token，会调用 `/api/auth/me` 恢复登录态。
- 文件上传组件限制 PDF，且客户端额外限制最大 50MB。
- 文件列表支持下载、单个删除、批量删除。
- 下载通过 Blob URL 触发浏览器下载。

## 已知注意事项

- README 和部分源码注释包含日文/中文内容，但在当前 PowerShell 输出中出现编码乱码；编辑这些文件时要注意保持 UTF-8。
- `AGENTS.md` 是本次新增的项目说明文件。
- `target/` 目录存在于仓库根目录，但通常是构建产物，不应作为业务源码维护。
- S3 object key 当前直接使用 DB 主键字符串，不单独保存 `s3_key` 字段。
- `files` 表不保存文件二进制，只有 metadata。
- 目前只允许 PDF 上传，前后端都做了限制。
- 删除是逻辑删除：DB 状态改为 `DELETED`，同时尝试删除 S3 object。
- `FileListService` 列表只读取 DB metadata，不访问 S3。
- `FileDownloadService` 会在 DB metadata 存在但 S3 object 不存在时记录 `DOWNLOAD / NOT_FOUND`。
- `GlobalExceptionHandler` 会把业务异常转换为统一 `ErrorResponse`。
- 前端错误展示依赖后端返回的 `ErrorResponse.message`。

## 后续开发建议

- 为认证、文件权限、上传失败、下载失败、删除权限补充后端测试。
- 为 S3 操作引入 mock/stub，避免单元测试依赖真实 AWS。
- 增加 `GET /api/files/{fileId}/logs`，展示审计日志。
- 明确 `ADMIN` 是否应该拥有删除他人文件的权限；当前实现没有。
- 为上传大小限制增加后端校验，当前明确的 50MB 限制主要在前端。
- 考虑 Presigned URL 方案，以降低后端传输大文件时的带宽压力。
- 根据部署地址调整 CORS allowed origins。
