import type { Metadata, Viewport } from "next";
import "./globals.css";
import { AppShell } from "@/components/layout/app-shell";

export const metadata: Metadata = {
  title: "LifeOS · Personal Operating System",
  description: "本地优先的个人工作台 —— 管理项目、任务、习惯、记录与复盘。",
};

export const viewport: Viewport = {
  themeColor: "#F8F5EE",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh" suppressHydrationWarning>
      <body>
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
