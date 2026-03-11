import { defineConfig } from '@vben/vite-config';
import { resolve } from 'path';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      resolve: {
        alias: {
          '@': resolve(__dirname, './src'),
        },
      },
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            // mock代理目标地址
            target: 'http://localhost:5320/api',
            ws: true,
          },
        },
      },
      build: {
        rollupOptions: {
          external: ['jiti'],
        },
      },
    },
  };
});
