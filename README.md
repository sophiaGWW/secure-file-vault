# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

Spring Boot と AWS S3 を使った、権限制御付きファイル管理システムの個人開発プロジェクトです。日本の IT 面接で説明しやすいように、JWT 認証、S3 Presigned URL、ユーザー単位のアクセス制御、DB メタデータ管理、操作ログを段階的に実装します。

## フェーズ 1 の範囲

今回のフェーズでは、最小構成のプロジェクト骨格だけを作成しています。

- `backend/`: Java 17 + Spring Boot 3 のバックエンド骨格
- `frontend/`: React + Vite のフロントエンド骨格
- `GET /api/health` が `OK` を返す
- フロントエンドのトップページに `Secure File Vault` を表示
- README 初版を追加

ログイン認証はフェーズ 2 で実装済みです。S3 連携とファイルアップロードはまだ実装していません。

## 技術スタック予定

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

フェーズ 2 では、Spring Security + JWT によるログイン認証を実装しています。

バックエンド API:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

認証フロー:

1. ユーザーは `POST /api/auth/register` でアカウントを登録します。
2. バックエンドは BCrypt で password hash を保存し、平文パスワードは保存しません。
3. 登録またはログインに成功すると、バックエンドは JWT を返します。
4. フロントエンドは JWT を `localStorage` に保存します。
5. 認証が必要な API には、以下のリクエストヘッダーを付けます。

```text
Authorization: Bearer <token>
```

6. バックエンドの JWT Filter が token を検証し、現在のユーザーを読み込みます。
7. 未ログインユーザーは `GET /api/auth/me` や FileDashboard にアクセスできません。

`users` テーブル:

```text
id
username
password_hash
role
created_at
```

バックエンド起動前に、MySQL のデータベースが存在することを確認してください。

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

デフォルト設定:

```text
DB_URL=jdbc:mysql://localhost:3306/secure_file_vault?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=dev-only-change-me-secure-file-vault-jwt-secret-please-override
```

面接やデモでは、`JWT_SECRET` を 32 文字以上のランダム文字列に変更してください。

## 起動方法

### Backend

```bash
cd backend
mvn spring-boot:run
```

確認 URL:

```text
http://localhost:8080/api/health
```

期待結果:

```text
OK
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite の表示する URL をブラウザで開くと、トップページに `Secure File Vault` が表示されます。

## プロジェクト構成

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

## 設計方針

- まずはバックエンドとフロントエンドを分離し、後続フェーズで機能を追加しやすくします。
- S3 bucket は private 前提で設計します。
- ファイルのアップロードとダウンロードは、後続フェーズでバックエンドが発行する Presigned URL 経由にします。
- 認証情報や AWS access key はコードに直接書かず、環境変数または設定ファイルから読み込みます。

## 次のフェーズ

次はファイルアップロード入口とファイルメタデータ管理を実装します。

- `files` テーブル
- ファイル状態 `UPLOADING / AVAILABLE / DELETED / FAILED`
- S3 Presigned Upload URL の生成
- アップロード完了後のバックエンド通知
- 現在のユーザーのファイルだけを一覧表示
