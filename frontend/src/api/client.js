// API のベース URL。ローカル開発では Spring Boot の 8080 番を使う。
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export async function apiRequest(path, options = {}) {
  // FormData の場合はブラウザに boundary を自動設定させるため Content-Type を付けない。
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
  const token = localStorage.getItem('secure_file_vault_token');
  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...options.headers,
  };

  if (token) {
    // ログイン済みの場合は JWT を Authorization ヘッダーに付与する。
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  const data = await readResponse(response);

  if (!response.ok) {
    // バックエンドの ErrorResponse.message を画面表示用エラーとして使う。
    throw new Error(data.message || 'リクエストに失敗しました。');
  }

  return data;
}

export async function apiBlobRequest(path, options = {}) {
  // PDF ダウンロード用。JSON ではなく Blob としてレスポンスを扱う。
  const token = localStorage.getItem('secure_file_vault_token');
  const headers = {
    ...options.headers,
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const data = await readResponse(response);
    throw new Error(data.message || 'リクエストに失敗しました。');
  }

  return response.blob();
}

async function readResponse(response) {
  // 204 No Content など body が空の場合でも呼び出し側が扱いやすいよう空 object にする。
  const text = await response.text();
  if (!text) {
    return {};
  }
  return JSON.parse(text);
}
