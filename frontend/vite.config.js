import { defineConfig } from 'vite';

export default defineConfig({
    build: {
        outDir: '../src/main/resources/static/dist',  // Spring Boot static 폴더로 출력
        rollupOptions: {
            input: 'src/main.js',
            output: {
                entryFileNames: 'ckeditor-bundle.js',
                chunkFileNames: '[name].js',
                assetFileNames: '[name].[ext]'
            }
        }
    }
});