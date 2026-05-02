# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

A personal portfolio project for a permission-controlled file management system built with Spring Boot and AWS S3. This version simulates a legacy-system migration: PDF files that used to be stored as DB BLOBs are now stored in AWS S3, while the DB keeps only file metadata. The project is designed to be easy to explain in Japanese IT interviews, including JWT authentication, backend multipart upload, user-level access control, database-managed file metadata, and operation logs.

## Phase 1 Scope

This phase only creates the minimum project skeleton:

- `backend/`: Java 17 + Spring Boot 3 backend skeleton
- `frontend/`: React + Vite frontend skeleton
- `GET /api/health` returns `OK`
- The frontend home page displays `Secure File Vault`
- Initial README files

The current phase implements backend multipart upload to S3, backend download, and backend delete APIs.

## Planned Tech Stack

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

Phase 2 implements login authentication with Spring Security + JWT.

Backend APIs:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

Authentication flow:

1. A user registers with `POST /api/auth/register`.
2. The backend stores a BCrypt password hash, never the plain password.
3. Register and login responses return a JWT.
4. The frontend stores the JWT in `localStorage`.
5. Protected API requests include this header:

```text
Authorization: Bearer <token>
```

6. The backend JWT filter validates the token and loads the current user.
7. Unauthenticated users cannot access `GET /api/auth/me` or the file dashboard.

`users` table:

```text
id
username
password_hash
role
created_at
```

Before starting the backend, make sure the MySQL database exists:

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Default configuration:

```text
DB_URL=jdbc:mysql://localhost:3306/secure_file_vault?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=dev-only-change-me-secure-file-vault-jwt-secret-please-override
```

For interviews or real demos, replace `JWT_SECRET` with a random string of at least 32 characters.

## S3 Backend Upload

The current implementation simulates migrating an existing system from DB BLOB storage to AWS S3 storage. To reduce frontend migration scope, the frontend still uploads with `multipart/form-data`; the backend receives the file and uploads the file body to a private S3 bucket.

Backend APIs:

```text
POST   /api/files/upload
GET    /api/files
GET    /api/files/{fileId}/download
DELETE /api/files/{fileId}
```

Upload flow:

1. The frontend selects a PDF file.
2. The frontend calls `POST /api/files/upload` with `multipart/form-data`.
3. `FileController` only receives requests and returns responses.
4. `FileUploadService` validates that the file is not empty and the Content-Type is `application/pdf`.
5. The backend inserts DB metadata first with status `UPLOADING`.
6. The generated DB `id` is used as the S3 object key.
7. `FileUploadService` calls `S3StorageService.upload(...)` to store the file body in S3.
8. After upload succeeds, the DB status becomes `AVAILABLE` and an operation log is written.

Download and delete flow:

- `GET /api/files` returns metadata only, never the file body. Regular users can see only files whose `owner_id` matches their userId; `ADMIN` users can see all non-deleted files.
- `FileDownloadService` queries DB metadata by `fileId`, checks that the file exists, checks `status = AVAILABLE`, and validates access by `owner_id`.
- Regular users can download only files whose `owner_id` matches their userId; `ADMIN` can download every file.
- After authorization succeeds, the backend uses `String.valueOf(fileId)` as the S3 object key and calls `S3StorageService.download(objectKey)` to fetch the file from S3.
- The download response uses `application/pdf` and sets the `Content-Disposition` filename from `original_filename`.
- Successful downloads write a `DOWNLOAD / SUCCESS` log; missing files write `DOWNLOAD / NOT_FOUND`; unauthorized access writes `DOWNLOAD / ACCESS_DENIED`.
- `FileDeleteService` queries DB by `fileId`, checks ownership, calls `S3StorageService.delete(s3Key)`, and writes a delete log.

Allowed content types:

```text
application/pdf
```

S3 object key rule:

```text
objectKey = String.valueOf(fileId)
```

For example, if the DB primary key is `123`, the S3 object key is also `123`.

The `files` table stores metadata only, not file binary data:

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

`S3StorageService` encapsulates all S3 operations for reuse across business services:

```text
upload(String s3Key, InputStream inputStream, long contentLength, String contentType)
download(String objectKey)
delete(String s3Key)
exists(String s3Key)
```

Presigned URLs remain a future optimization option. They can be introduced later if backend bandwidth needs to be reduced by letting the browser upload directly to S3.

AWS configuration comes from environment variables or `application.yml`:

```text
AWS_REGION=ap-northeast-1
AWS_S3_BUCKET=your-private-bucket-name
```

AWS access keys are not hard-coded. For local development, use an AWS CLI profile or environment variables:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

Because the current upload flow goes through the backend, local upload testing does not require S3 CORS.

## Run

### Backend

```bash
cd backend
mvn spring-boot:run
```

Health check:

```text
http://localhost:8080/api/health
```

Expected response:

```text
OK
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Open the URL printed by Vite. The home page should display `Secure File Vault`.

## Project Structure

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

## Design Notes

- Backend and frontend are separated first so later phases can be added cleanly.
- The S3 bucket is designed as private.
- Backend upload is used to simulate a legacy-system migration and reduce frontend changes.
- The DB stores only metadata; the file body is stored in S3.
- The S3 object key uses the DB primary key, so no separate `s3_key` column is needed.
- `S3StorageService` encapsulates upload, download, delete, and existence checks for reuse.
- Authentication secrets and AWS access keys must not be hard-coded. They should come from environment variables or configuration.

## Next Phase

Possible next improvements:

- `GET /api/files/{fileId}/logs`
- Admin access control and cross-user audit views
- Presigned URL upload to reduce backend file-transfer bandwidth
