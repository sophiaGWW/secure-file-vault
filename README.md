# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

Java 17 + Spring Boot 3 で構築する、AWS S3 ベースの権限制御付きファイル管理システムです。

## フェーズ 1 の範囲

- Spring Boot アプリケーションが起動できること。
- `GET /api/health` が `OK` を返すこと。
- MySQL のデータソース設定。
- MyBatis Starter と Mapper Scan の設定。
- 初期スキーマで以下の 3 テーブルを作成:
  - `users`
  - `files`
  - `file_access_logs`
- Swagger / OpenAPI UI の有効化。

## 必要環境

- Java 17
- Maven 3.9+
- MySQL 8+

## データベース準備

アプリケーション起動前に、MySQL でデータベースを作成します。

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

デフォルトでは、以下の接続設定を使用します。

```text
DB_URL=jdbc:mysql://localhost:3306/secure_file_vault?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=password
```

必要に応じて環境変数で上書きしてください。

## 起動方法

```bash
mvn spring-boot:run
```

起動後、以下にアクセスできます。

- ヘルスチェック: `http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## プロジェクト構成

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

## メモ

このバージョンは、最小限の起動可能なフェーズ 1 に限定しています。ファイルアップロード、S3 連携、認証、認可、アクセスログの実装は後続フェーズで追加します。
