import { apiRequest } from './client.js';

export function register(form) {
  // ユーザー登録後、バックエンドから JWT とユーザー情報を受け取る。
  return apiRequest('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export function login(form) {
  // ログイン成功時に JWT とユーザー情報を受け取る。
  return apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export function getCurrentUser(token) {
  // ページ再読み込み時に localStorage の token がまだ有効か確認する。
  return apiRequest('/api/auth/me', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}
