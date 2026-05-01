# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

基于 Java 17 + Spring Boot 3 构建的 AWS S3 权限控制文件管理系统。

## 第一阶段范围

- Spring Boot 应用可以启动。
- `GET /api/health` 返回 `OK`。
- 配置 MySQL 数据源。
- 配置 MyBatis Starter 和 Mapper 扫描。
- 初始化数据库结构，创建以下三张表：
  - `users`
  - `files`
  - `file_access_logs`
- 启用 Swagger / OpenAPI UI。

## 环境要求

- Java 17
- Maven 3.9+
- MySQL 8+

## 数据库准备

启动应用前，先在 MySQL 中创建数据库：

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

应用默认使用以下连接配置：

```text
DB_URL=jdbc:mysql://localhost:3306/secure_file_vault?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=password
```

如有需要，可以通过环境变量覆盖这些配置。

## 启动方法

```bash
mvn spring-boot:run
```

启动后访问：

- 健康检查：`http://localhost:8080/api/health`
- Swagger UI：`http://localhost:8080/swagger-ui.html`

## 项目结构

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

## 说明

当前版本只实现第一阶段的最小可运行范围。文件上传、S3 集成、认证、授权和访问日志业务逻辑会在后续阶段继续添加。
