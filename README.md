# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

Spring Boot と AWS S3 を使った、権限制御付きファイル管理システムの個人開発プロジェクトです。既存システム改修の想定として、これまで DB BLOB に保存していた PDF ファイルを AWS S3 保存へ移行し、DB には metadata だけを保存します。日本の IT 面接で説明しやすいように、JWT 認証、バックエンド経由の multipart アップロード、ユーザー単位のアクセス制御、DB メタデータ管理、操作ログを段階的に実装します。

## フェーズ 1 の範囲

今回のフェーズでは、最小構成のプロジェクト骨格だけを作成しています。

- `backend/`: Java 17 + Spring Boot 3 のバックエンド骨格
- `frontend/`: React + Vite のフロントエンド骨格
- `GET /api/health` が `OK` を返す
- フロントエンドのトップページに `Secure File Vault` を表示
- README 初版を追加

現在のフェーズでは、バックエンドが multipart/form-data を受け取り S3 へアップロードする処理、ダウンロード、削除 API を実装しています。

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

## S3 Backend Upload

現在の実装では、既存システムの DB BLOB 保存から AWS S3 保存への移行をシミュレーションしています。既存フロントエンドの改修範囲を小さくするため、フロントエンドは `multipart/form-data` でバックエンドへアップロードし、ファイル本体はバックエンドが private S3 bucket へ保存します。

バックエンド API:

```text
POST   /api/files/upload
GET    /api/files
GET    /api/files/{fileId}/download
DELETE /api/files/{fileId}
```

アップロードフロー:

1. フロントエンドで PDF ファイルを選択します。
2. フロントエンドが `multipart/form-data` で `POST /api/files/upload` を呼び出します。
3. `FileController` はリクエスト受け取りとレスポンス返却だけを担当します。
4. `FileUploadService` が空ファイルと Content-Type `application/pdf` を検証します。
5. バックエンドが DB metadata を `UPLOADING` で先に登録します。
6. DB で生成された `id` を S3 object key として使います。
7. `FileUploadService` が `S3StorageService.upload(...)` を呼び出して S3 へ保存します。
8. アップロード成功後、DB 状態を `AVAILABLE` に更新し、操作ログを記録します。

ダウンロードと削除:

- `GET /api/files` は metadata のみを返し、ファイル本体は返しません。一般ユーザーは `owner_id` が自分の userId と一致するファイルだけを参照でき、`ADMIN` は削除済み以外の全ファイルを参照できます。
- `FileDownloadService` は `fileId` で DB metadata を検索し、存在確認、`status = AVAILABLE`、`owner_id` による権限チェックを行います。
- 一般ユーザーは `owner_id` が自分の userId と一致するファイルだけをダウンロードでき、`ADMIN` は全ファイルをダウンロードできます。
- 権限チェック後、バックエンドは `String.valueOf(fileId)` を S3 object key として使い、`S3StorageService.download(objectKey)` で S3 からファイルを取得します。
- ダウンロードレスポンスは `application/pdf` とし、`original_filename` を使って `Content-Disposition` のファイル名を設定します。
- ダウンロード成功時は `DOWNLOAD / SUCCESS`、ファイル不存在時は `DOWNLOAD / NOT_FOUND`、権限なしの場合は `DOWNLOAD / ACCESS_DENIED` をログに記録します。
- `FileDeleteService` は `fileId` で DB を検索し、権限チェック後に `S3StorageService.delete(s3Key)` を呼び出し、削除ログを記録します。

許可する content type:

```text
application/pdf
```

S3 object key のルール:

```text
objectKey = String.valueOf(fileId)
```

たとえば DB 主キーが `123` の場合、S3 object key も `123` です。

`files` テーブルはファイル本体を保存せず、metadata だけを保存します。

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

`S3StorageService` は S3 操作を共通化し、複数の業務 Service から再利用できるようにしています。

```text
upload(String s3Key, InputStream inputStream, long contentLength, String contentType)
download(String objectKey)
delete(String s3Key)
exists(String s3Key)
```

Presigned URL は将来の最適化案です。バックエンドの転送帯域を下げたい場合、後続フェーズでブラウザから S3 へ直接アップロードする方式に変更できます。

AWS 設定は環境変数または `application.yml` から読み込みます。

```text
AWS_REGION=ap-northeast-1
AWS_S3_BUCKET=your-private-bucket-name
```

AWS access key はコードに書きません。ローカル開発では AWS CLI profile または環境変数を使用します。

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

現在のアップロード方式ではブラウザが S3 に直接アクセスしないため、ローカルアップロード確認に S3 CORS は不要です。

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
- バックエンド経由のアップロードは、既存システム改修を想定し、既存フロントエンドの改修範囲を小さくするために採用しています。
- DB は metadata のみを保存し、ファイル本体は S3 に保存します。
- S3 object key は DB 主キーを使用し、別途 `s3_key` は保存しません。
- `S3StorageService` がアップロード、ダウンロード、削除、存在確認を共通化します。
- 認証情報や AWS access key はコードに直接書かず、環境変数または設定ファイルから読み込みます。

## 次のフェーズ

次は以下の改善が考えられます。

- `GET /api/files/{fileId}/logs`
- 管理者権限とユーザー横断の監査
- バックエンドの転送帯域を削減する Presigned URL アップロード
