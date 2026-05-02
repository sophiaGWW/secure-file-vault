import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// React plugin を有効化した Vite 開発サーバー設定。
export default defineConfig({
  plugins: [react()],
});
