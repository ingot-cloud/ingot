import { fileURLToPath, URL } from "node:url";
import { resolve } from "path";
import { defineConfig, loadEnv } from "vite";
import { createHtmlPlugin } from "vite-plugin-html";
import vue from "@vitejs/plugin-vue";

// https://vitejs.dev/config/
export default defineConfig(({ mode, command, ssrBuild }) => {
  const getViteEnv = (target: string) => {
    return loadEnv(mode, process.cwd())[target];
  };
  return {
    plugins: [
      vue(),
      createHtmlPlugin({
        inject: {
          data: {
            //将环境变量 VITE_APP_TITLE 赋值给 title 方便 html页面使用 title 获取系统标题
            title: getViteEnv("VITE_APP_TITLE"),
          },
        },
      }),
    ],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
        vue: "vue/dist/vue.esm-bundler.js",
      },
    },
    server: {
      port: 5173,
    },
    build: {
      outDir: resolve(
        __dirname,
        "../../ingot-service/ingot-auth/src/main/resources"
      ),
      rollupOptions: {
        input: {
          entry: resolve(__dirname, "index.html"),
          // login: resolve(__dirname, "src/views/login/index.html"),
        },
        output: {
          chunkFileNames: "static/js/[name]-[hash].js",
          entryFileNames: "static/js/[name]-[hash].js",
          assetFileNames: "static/[ext]/[name]-[hash].[ext]",
        },
      },
    },
  };
});
