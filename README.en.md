# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

A personal portfolio project for a permission-controlled file management system built with Spring Boot and AWS S3. The project will be implemented in stages so it can be explained clearly in Japanese IT interviews, including JWT authentication, S3 Presigned URLs, user-level access control, database-managed file metadata, and operation logs.

## Phase 1 Scope

This phase only creates the minimum project skeleton:

- `backend/`: Java 17 + Spring Boot 3 backend skeleton
- `frontend/`: React + Vite frontend skeleton
- `GET /api/health` returns `OK`
- The frontend home page displays `Secure File Vault`
- Initial README files

Login authentication is implemented in phase 2. S3 integration and file upload are not implemented yet.

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
- The S3 bucket will be designed as private.
- Upload and download flows will use backend-generated Presigned URLs in later phases.
- Authentication secrets and AWS access keys must not be hard-coded. They should come from environment variables or configuration.

## Next Phase

The next phase should implement the file upload entry point and file metadata table:

- `files` table
- File statuses: `UPLOADING / AVAILABLE / DELETED / FAILED`
- Generate S3 Presigned Upload URLs
- Notify backend after upload completion
- List only the current user's files
