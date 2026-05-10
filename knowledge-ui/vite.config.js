import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: true,  // 允许所有域名（local、内网IP、ngrok等）
    /*allowedHosts: ['77nafjnpks6a.ngrok.xiaomiqiu123.top',
    'jgcszhuz3sxl.ngrok.xiaomiqiu123.top'],*/
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: true, // 如果 ngrok 使用 HTTPS

        configure: (proxy, options) => {
            proxy.on('error', (err, req, res) => {
                console.log('proxy error', err);
            });
        }
      }
    }
  }
})
