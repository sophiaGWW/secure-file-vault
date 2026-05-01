# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

Java 17 + Spring Boot 3 project for an AWS S3 based permission-controlled file management system.

## Phase 1 Scope

- Spring Boot application can start.
- `GET /api/health` returns `OK`.
- MySQL datasource configuration.
- MyBatis starter and mapper scanning are configured.
- Initial schema creates:
  - `users`
  - `files`
  - `file_access_logs`
- Swagger / OpenAPI UI is enabled.

## Requirements

- Java 17
- Maven 3.9+
- MySQL 8+

## Database Setup

Create the database before starting the application:

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

By default the application uses:

```text
DB_URL=jdbc:mysql://localhost:3306/secure_file_vault?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=password
```

Override them with environment variables if needed.

## Run

```bash
mvn spring-boot:run
```

Then open:

- Health check: `http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Project Structure

```text
src/main/java/com/example/securefilevault
  config/OpenApiConfig.java
  controller/HealthController.java
  SecureFileVaultApplication.java

src/main/resources
  application.yml
  schema.sql

src/test/java/com/example/securefilevault
  SecureFileVaultApplicationTests.java
```

## Notes

This version is intentionally limited to the first runnable phase. File upload, S3 integration, authentication, authorization, and access logging behavior will be added in later phases.
