import { readFileSync } from "node:fs";

const { version } = JSON.parse(readFileSync(new URL("./package.json", import.meta.url), "utf8"));

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // 静态导出到 out/,供 Capacitor 打包进 Android APK
  output: "export",
  images: { unoptimized: true },
  // 版本号唯一来源是 package.json(Android 的 versionName 也从这里读)
  env: { NEXT_PUBLIC_APP_VERSION: version },
};

export default nextConfig;
