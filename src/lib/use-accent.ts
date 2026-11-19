"use client";

import { useAppStore } from "@/store/app.store";
import { accentVars } from "@/lib/accent";
import { resolvedTheme } from "@/lib/use-theme";

/**
 * 当前生效主题色的真实颜色值，供 SVG 属性使用。
 * Android WebView 对 SVG 属性里的 CSS 变量解析不完整，图表统一用真实 rgb 值。
 */
export interface AccentColors {
  /** "85, 107, 47" —— 可直接拼进 rgb()/rgba()。 */
  rgb: string;
  /** "rgb(85, 107, 47)" —— 可直接作为 SVG 的 stroke/fill/stop-color。 */
  css: string;
  axis: string;
  grid: string;
  surface: string;
}

const DEFAULT_RGB = {
  light: "85, 107, 47",
  dark: "154, 168, 106",
};

const NEUTRAL = {
  light: { axis: "rgb(119, 119, 119)", grid: "rgb(230, 225, 215)", surface: "rgb(255, 253, 248)" },
  dark: { axis: "rgb(161, 161, 170)", grid: "rgb(63, 63, 70)", surface: "rgb(39, 39, 42)" },
};

/** React 组件里用：订阅主题与主题色，改色后自动带着新值重渲染。 */
export function useAccentColors(): AccentColors {
  const mode = useAppStore((s) => s.settings.themeMode);
  const hue = useAppStore((s) => s.settings.accentHue);
  const sat = useAppStore((s) => s.settings.accentSat);
  const light = useAppStore((s) => s.settings.accentLight);
  const theme = resolvedTheme(mode);
  const rgb = hue == null || !Number.isFinite(hue)
    ? DEFAULT_RGB[theme]
    : accentVars(hue, theme, sat, light).primary.trim().split(/\s+/).join(", ");
  return { rgb, css: `rgb(${rgb})`, ...NEUTRAL[theme] };
}
