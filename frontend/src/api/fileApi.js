import { apiBlobRequest, apiRequest } from './client.js';

export function uploadFile(file) {
  const formData = new FormData();
  formData.append('file', file);

  return apiRequest('/api/files/upload', {
    method: 'POST',
    body: formData,
  });
}

export function downloadFile(fileId) {
  return apiBlobRequest(`/api/files/${fileId}/download`);
}

export function deleteFile(fileId) {
  return apiRequest(`/api/files/${fileId}`, {
    method: 'DELETE',
  });
}

export function listFiles() {
  return apiRequest('/api/files');
}
