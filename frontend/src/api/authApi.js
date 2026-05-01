import { apiRequest } from './client.js';

export function register(form) {
  return apiRequest('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export function login(form) {
  return apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export function getCurrentUser(token) {
  return apiRequest('/api/auth/me', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}
