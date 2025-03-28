import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/accounts': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/transactions': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/transfers': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/subscriptions': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/notifications': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:15674',
        changeOrigin: true,
        ws: true
      }
    }
  }
});
