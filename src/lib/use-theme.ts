"use client";

import { useEffect } from "react";
import { useAppStore } from "@/store/app.store";
import { accentVars } from "@/lib/accent";
import type { ThemeMode } from "@/types";

/** 把 system 解析成实际浅/深（SSR 下按浅色，客户端 hydrate 后由 effect 校正）。 */
export function resolvedTheme(mode: ThemeMode): "light" | "dark" {
  if (mode === "system" && typeof window !== "undefined") {
    return window.matchMedia?.("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }
  return mode === "dark" ? "dark" : "light";
}

function apply(mode: ThemeMode, hue: number | null, sat: number | null, light: number | null) {
  if (typeof document === "undefined") return;
  const theme = resolvedTheme(mode);
  document.documentElement.classList.toggle("dark", theme === "dark");

  // 自定义主题色：运行时重写两个 CSS 变量；默认时清除内联值，回落 globals.css 的橄榄绿。
  const root = document.documentElement.style;
  if (hue == null || !Number.isFinite(hue)) {
    root.removeProperty("--primary");
    root.removeProperty("--primary-hover");
  } else {
    const vars = accentVars(hue, theme, sat, light);
    root.setProperty("--primary", vars.primary);
    root.setProperty("--primary-hover", vars.hover);
  }
}

/** 应用深浅色与自定义主题色，system 模式下跟随系统变化。 */
export function useTheme() {
  const mode = useAppStore((s) => s.settings.themeMode);
  const hue = useAppStore((s) => s.settings.accentHue);
  const sat = useAppStore((s) => s.settings.accentSat);
  const light = useAppStore((s) => s.settings.accentLight);

  useEffect(() => {
    apply(mode, hue, sat, light);
    if (mode !== "system") return;
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => apply("system", hue, sat, light);
    mq.addEventListener?.("change", onChange);
    return () => mq.removeEventListener?.("change", onChange);
  }, [mode, hue, sat, light]);
}
