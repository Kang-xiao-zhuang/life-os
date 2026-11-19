"use client";

import { useEffect } from "react";
import { useAppStore } from "@/store/app.store";
import type { ThemeMode } from "@/types";

function systemDark(): boolean {
  return typeof window !== "undefined" && window.matchMedia?.("(prefers-color-scheme: dark)").matches;
}

function apply(mode: ThemeMode) {
  const dark = mode === "dark" || (mode === "system" && systemDark());
  document.documentElement.classList.toggle("dark", dark);
}

/** 根据设置应用深浅色，并跟随系统主题变化（当设为 system 时）。 */
export function useTheme() {
  const mode = useAppStore((s) => s.settings.themeMode);

  useEffect(() => {
    apply(mode);
    if (mode !== "system") return;
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => apply("system");
    mq.addEventListener?.("change", onChange);
    return () => mq.removeEventListener?.("change", onChange);
  }, [mode]);
}
