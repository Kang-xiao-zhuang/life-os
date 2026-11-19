import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  test: {
    include: ["src/**/*.test.ts"],
    // 固定时区:测试「本地日期」逻辑时结果不随机器变化
    env: { TZ: "Asia/Shanghai" },
  },
});
