# Secure File Vault

[日本語](README.md) | [中文](README.zh-CN.md) | [English](README.en.md)

Secure File Vault は、PDF ファイルを安全にアップロード、管理、ダウンロード、削除するための、権限制御付きファイル管理システムです。フロントエンドとバックエンドを分離した構成で、フロントエンドは Netlify、バックエンドは Render にデプロイしています。データベースは AWS RDS MySQL、ファイル本体は private な AWS S3 bucket に保存し、DB にはユーザー情報、ファイル metadata、アクセス監査ログだけを保存します。

公開 URL:

```text
https://imaginative-pavlova-90e5f8.netlify.app/
```

<img width="1927" height="1055" alt="image" src="https://github.com/user-attachments/assets/779623ca-745b-4d04-916d-5969dfe00346" />
<br/>
<img width="2536" height="1247" alt="image" src="https://github.com/user-attachments/assets/964344e0-41fd-4c56-880b-36196cf9598a" />

## 主な機能

- ユーザー登録、ログイン、ログアウト
- JWT 認証による保護 API へのアクセス
- ブラウザに保存された token によるログイン状態の復元
- PDF ファイルアップロード。フロントエンドとバックエンドの両方で `application/pdf` を検証
- 最大アップロードサイズ 500MB
- ファイル一覧表示：ファイル名、所有者 ID、Content-Type、サイズ、状態、作成日時
- バックエンド経由の PDF ダウンロード。ファイル本体は S3 から取得
- 単一ファイル削除
- 複数選択による一括削除
- ユーザー単位の権限制御：一般ユーザーは自分のファイルのみアクセス可能
- 管理者は削除済み以外の全ファイルを表示、ダウンロード可能
- 論理削除：S3 object は削除し、DB metadata は `DELETED` として保持
- アップロード、ダウンロード、削除操作の監査ログ
- 統一されたエラーレスポンスとフロントエンドでのエラー表示
- Swagger UI / OpenAPI ドキュメント

## 技術スタックと利用プラットフォーム

Frontend:

- React 19
- Vite 7
- JavaScript
- Plain CSS
- Browser `fetch`
- JWT は `localStorage` に保存
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
- フロントエンド origin 向けの CORS 設定

## システム構成

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

ファイルは DB BLOB として保存しません。アップロード時は、バックエンドが先にファイル metadata を作成し、生成された `files.id` を S3 object key としてファイル本体を S3 に保存します。ダウンロード時は、DB metadata、ファイル状態、ユーザー権限を確認した後、S3 からファイル本体を取得してブラウザへ返します。

## API 一覧

公開 API:

| Method | Endpoint | 説明 |
| --- | --- | --- |
| `GET` | `/api/health` | バックエンドのヘルスチェック |
| `POST` | `/api/auth/register` | ユーザー登録 |
| `POST` | `/api/auth/login` | ログイン |
| `GET` | `/swagger-ui.html` | Swagger UI |

JWT が必要な API:

| Method | Endpoint | 説明 |
| --- | --- | --- |
| `GET` | `/api/auth/me` | 現在のログインユーザー取得 |
| `POST` | `/api/files/upload` | PDF ファイルアップロード |
| `GET` | `/api/files` | ファイル一覧取得 |
| `GET` | `/api/files/{fileId}/download` | ファイルダウンロード |
| `DELETE` | `/api/files/{fileId}` | ファイル削除 |

保護 API には以下のヘッダーが必要です。

```text
Authorization: Bearer <token>
```

## 権限ルール

- 未ログインユーザーはヘルスチェック、登録、ログイン、OpenAPI ドキュメントのみアクセス可能。
- 一般 `USER` は自分がアップロードしたファイルだけを表示、ダウンロード、削除可能。
- `ADMIN` は削除済み以外の全ファイルを表示、ダウンロード可能。
- ファイル削除は owner のみ実行可能。
- `AVAILABLE` 以外の状態のファイルはダウンロード不可。
- 削除済みファイルはダウンロード対象として提供しない。

## ファイルライフサイクル

ファイル状態:

```text
UPLOADING
AVAILABLE
FAILED
DELETED
```

アップロード処理:

1. フロントエンドで PDF ファイルを選択します。
2. フロントエンドが `multipart/form-data` で `/api/files/upload` を呼び出します。
3. バックエンドがファイル名、空ファイル、Content-Type を検証します。
4. バックエンドが `files` テーブルへ metadata を登録し、状態を `UPLOADING` にします。
5. 生成された `files.id` を S3 object key として使用します。
6. AWS SDK 経由でファイル本体を S3 にアップロードします。
7. アップロード成功後、状態を `AVAILABLE` に更新します。
8. アップロード失敗時は、状態を `FAILED` に更新します。
9. アップロード結果を `file_access_logs` に記録します。

ダウンロード処理:

1. バックエンドが `fileId` で metadata を検索します。
2. ファイルが存在し、状態が `AVAILABLE` であることを確認します。
3. 現在のユーザーにダウンロード権限があるか確認します。
4. `String.valueOf(fileId)` を使って S3 から object を取得します。
5. `application/pdf` として返却し、元のファイル名を設定します。
6. ダウンロード成功または失敗を監査ログに記録します。

削除処理:

1. バックエンドが `fileId` で metadata を検索します。
2. ファイルが存在し、未削除であることを確認します。
3. 現在のユーザーが owner であることを確認します。
4. 対応する S3 object を削除します。
5. DB metadata の状態を `DELETED` に更新します。
6. 削除成功または失敗を監査ログに記録します。

## データベース

初期化スクリプト:

```text
backend/src/main/resources/schema.sql
```

主なテーブル:

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

監査ログで使用する action / result:

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

## 環境変数

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

ローカル開発用のデフォルト値は以下で定義しています。

```text
backend/src/main/resources/application.yml
frontend/src/api/client.js
```

本番環境ではデフォルトの `JWT_SECRET` を使用せず、DB password や AWS access key をソースコードにコミットしないでください。

## ローカル開発

データベース作成:

```sql
CREATE DATABASE secure_file_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

バックエンド起動:

```bash
cd backend
mvn spring-boot:run
```

ヘルスチェック:

```text
http://localhost:8080/api/health
```

フロントエンド起動:

```bash
cd frontend
npm install
npm run dev
```

デフォルトのフロントエンド URL:

```text
http://localhost:5173
```

フロントエンド build:

```bash
cd frontend
npm run build
```

バックエンド test:

```bash
cd backend
mvn test
```

## プロジェクト構成

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

## デプロイ

Frontend on Netlify:

- Build command: `npm run build`
- Publish directory: `frontend/dist`
- `VITE_API_BASE_URL` に Render のバックエンド URL を設定

Backend on Render:

- Java 17 / Spring Boot アプリケーション
- Render の port に対応するため `PORT` 環境変数を使用
- 環境変数経由で AWS RDS MySQL に接続
- 環境変数経由で S3 bucket、AWS region、AWS credentials を設定
- Netlify の domain を `CORS_ALLOWED_ORIGINS` に設定

AWS:

- RDS MySQL はユーザー、ファイル metadata、監査ログを保存
- S3 bucket は private access policy で運用
- ファイル object key は DB の file primary key を文字列化して使用
