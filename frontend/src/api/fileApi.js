import { apiBlobRequest, apiRequest } from './client.js';

export function uploadFile(file) {
  // 既存システム互換のため、ファイルは multipart/form-data でバックエンドへ送る。
  const formData = new FormData();
  formData.append('file', file);

  return apiRequest('/api/files/upload', {
    method: 'POST',
    body: formData,
  });
}

export function downloadFile(fileId) {
  // バックエンド経由で S3 の PDF を Blob として取得する。
  return apiBlobRequest(`/api/files/${fileId}/download`);
}

export function deleteFile(fileId) {
  // S3 object 削除と DB 論理削除をバックエンドに依頼する。
  return apiRequest(`/api/files/${fileId}`, {
    method: 'DELETE',
  });
}

export function listFiles() {
  // DB metadata から現在ユーザーのファイル一覧を取得する。
  return apiRequest('/api/files');
}
