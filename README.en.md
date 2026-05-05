# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

Secure File Vault is a deployed permission-controlled file management system for securely uploading, managing, downloading, and deleting PDF files. The application uses a separated frontend/backend architecture: the frontend is hosted on Netlify, the backend is hosted on Render, the database runs on AWS RDS MySQL, and file bodies are stored in a private AWS S3 bucket. The database stores users, file metadata, and audit logs only.

Live site:

```text
https://imaginative-pavlova-90e5f8.netlify.app/
```

## Features

- User registration, login, and logout
- JWT authentication for protected API access
- Login restoration from the token stored in the browser
- PDF upload with frontend and backend `application/pdf` validation
- Maximum upload size of 500MB
- File list with filename, owner ID, content type, size, status, and creation time
- PDF download through the backend, with the file body read from S3
- Single-file deletion
- Multi-select bulk deletion
- User-level authorization: regular users can access only their own files
- Admin users can view and download every non-deleted file
- Logical deletion: the S3 object is deleted while DB metadata remains as `DELETED`
- Audit logs for upload, download, and delete operations
- Unified error responses surfaced by the frontend
- Swagger UI / OpenAPI documentation

## Tech Stack And Platforms

Frontend:

- React 19
- Vite 7
- JavaScript
- Plain CSS
- Browser `fetch`
- JWT stored in `localStorage`
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
- CORS configured for the frontend origin

## Architecture

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

Files are not stored as database BLOBs. During upload, the backend creates file metadata first, then uses the generated `files.id` as the S3 object key. During download, the backend checks metadata, file status, and user permissions before reading the file body from S3 and returning it to the browser.

## API

Public endpoints:

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/health` | Backend health check |
| `POST` | `/api/auth/register` | Register a user |
| `POST` | `/api/auth/login` | Log in |
| `GET` | `/swagger-ui.html` | Swagger UI |

JWT-protected endpoints:

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/auth/me` | Get the current user |
| `POST` | `/api/files/upload` | Upload a PDF file |
| `GET` | `/api/files` | List files |
| `GET` | `/api/files/{fileId}/download` | Download a file |
| `DELETE` | `/api/files/{fileId}` | Delete a file |

Protected requests require this header:

```text
Authorization: Bearer <token>
```

## Authorization Rules

- Unauthenticated users can access only the health check, register, login, and OpenAPI documentation endpoints.
- Regular `USER` accounts can view, download, and delete only their own files.
- `ADMIN` accounts can view and download every non-deleted file.
- File deletion is allowed only for the file owner.
- Files whose status is not `AVAILABLE` cannot be downloaded.
- Deleted files are not served as downloadable files.

## File Lifecycle

File statuses:

```text
UPLOADING
AVAILABLE
FAILED
DELETED
```

Upload flow:

1. The frontend selects a PDF file.
2. The frontend calls `/api/files/upload` with `multipart/form-data`.
3. The backend validates the filename, empty file, and Content-Type.
4. The backend inserts metadata into the `files` table with status `UPLOADING`.
5. The backend uses the generated `files.id` as the S3 object key.
6. The backend uploads the file to S3 through the AWS SDK.
7. After a successful upload, the file status becomes `AVAILABLE`.
8. If upload fails, the file status becomes `FAILED`.
9. The upload result is written to `file_access_logs`.

Download flow:

1. The backend queries metadata by `fileId`.
2. It checks that the file exists and has status `AVAILABLE`.
3. It verifies that the current user has permission to download the file.
4. It reads the object from S3 using `String.valueOf(fileId)`.
5. It returns an `application/pdf` response with the original filename.
6. It writes a success or failure audit log.

Delete flow:

1. The backend queries metadata by `fileId`.
2. It checks that the file exists and is not already deleted.
3. It verifies that the current user is the owner.
4. It deletes the corresponding S3 object.
5. It updates the DB metadata status to `DELETED`.
6. It writes a success or failure audit log.

## Database

Schema file:

```text
backend/src/main/resources/schema.sql
```

Main tables:

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

Audit action / result values:

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

## Environment Variables

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

Local defaults are defined in:

```text
backend/src/main/resources/application.yml
frontend/src/api/client.js
```

Do not use the default `JWT_SECRET` in production, and do not commit database passwords or AWS access keys to source control.

## Local Development

Create the database:

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

Health check:

```text
http://localhost:8080/api/health
```

Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

Default frontend URL:

```text
http://localhost:5173
```

Build the frontend:

```bash
cd frontend
npm run build
```

Run backend tests:

```bash
cd backend
mvn test
```

## Project Structure

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

## Deployment

Frontend on Netlify:

- Build command: `npm run build`
- Publish directory: `frontend/dist`
- Configure `VITE_API_BASE_URL` to point to the Render backend

Backend on Render:

- Java 17 / Spring Boot application
- Uses the `PORT` environment variable for Render compatibility
- Connects to AWS RDS MySQL through environment variables
- Configures S3 bucket, AWS region, and AWS credentials through environment variables
- Requires the Netlify domain in `CORS_ALLOWED_ORIGINS`

AWS:

- RDS MySQL stores users, file metadata, and audit logs
- S3 bucket uses a private access policy
- File object keys use the DB file primary key as a string
