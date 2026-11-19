/**
 * 自定义主题色派生（移植自 Track100）。
 *
 * 界面层所有强调色走 Tailwind 的 primary token，底层是 --primary / --primary-hover
 * 两个 RGB 三元组 CSS 变量，浅深各一套。默认(accentHue=null)不写内联变量，
 * 直接沿用 globals.css 里的橄榄绿；选色后按当前主题运行时重写，切深浅时重新派生。
 *
 * 取色交互用 HSV（色相环 + 方形面板），持久化用 HSL 的 s/l，
 * 两者是同一个颜色的不同坐标，hsvToHsl / hslToHsv 负责无损互转。
 */

export type AppTheme = "light" | "dark";

/** 默认橄榄绿 #556B2F 的色相，仅作色环初始指针位置。 */
export const DEFAULT_ACCENT_HUE = 82;

/** 与 globals.css 中两套 --primary 对应的默认值，供设置页做精确数值反馈。 */
export const DEFAULT_ACCENT_HEX: Record<AppTheme, string> = {
  light: "#556B2F",
  dark: "#9AA86A",
};

interface Rgb {
  r: number;
  g: number;
  b: number;
}

const clamp01 = (n: number) => Math.max(0, Math.min(1, Number.isFinite(n) ? n : 0));

/** HSL → RGB，h/s/l 均为 0..1。 */
export function hslToRgb(h: number, s: number, l: number): Rgb {
  if (s === 0) {
    const v = Math.round(l * 255);
    return { r: v, g: v, b: v };
  }
  const hue = ((h % 1) + 1) % 1;
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
  const p = 2 * l - q;
  const channel = (t: number): number => {
    let k = t;
    if (k < 0) k += 1;
    if (k > 1) k -= 1;
    if (k < 1 / 6) return p + (q - p) * 6 * k;
    if (k < 1 / 2) return q;
    if (k < 2 / 3) return p + (q - p) * (2 / 3 - k) * 6;
    return p;
  };
  return {
    r: Math.round(channel(hue + 1 / 3) * 255),
    g: Math.round(channel(hue) * 255),
    b: Math.round(channel(hue - 1 / 3) * 255),
  };
}

/** HSV → HSL。方形取色板按 HSV 交互（x=饱和度，y=明度），存成 HSL 的 s/l。 */
export function hsvToHsl(s: number, v: number): { s: number; l: number } {
  const sv = clamp01(s);
  const vv = clamp01(v);
  const l = vv * (1 - sv / 2);
  const converted = l === 0 || l === 1 ? 0 : (vv - l) / Math.min(l, 1 - l);
  return { s: clamp01(converted), l: clamp01(l) };
}

/** HSL → HSV。把存储的 s/l 反算回方形取色板上的指针位置。 */
export function hslToHsv(s: number, l: number): { s: number; v: number } {
  const sl = clamp01(s);
  const ll = clamp01(l);
  const v = ll + sl * Math.min(ll, 1 - ll);
  const converted = v === 0 ? 0 : 2 * (1 - ll / v);
  return { s: clamp01(converted), v: clamp01(v) };
}

function rgbTriple(c: Rgb): string {
  return `${c.r} ${c.g} ${c.b}`;
}

function toHex(c: Rgb): string {
  const h = (n: number) => Math.max(0, Math.min(255, n)).toString(16).padStart(2, "0");
  return `#${h(c.r)}${h(c.g)}${h(c.b)}`;
}

export interface AccentVars {
  primary: string;
  hover: string;
}

/** 主题基准 s/l；自定义 sat/light 优先。浅色压深，深色提亮。 */
export function accentSL(theme: AppTheme, sat?: number | null, light?: number | null): { s: number; l: number } {
  const base = theme === "dark" ? { s: 0.44, l: 0.66 } : { s: 0.42, l: 0.32 };
  return {
    s: sat == null || !Number.isFinite(sat) ? base.s : clamp01(sat),
    l: light == null || !Number.isFinite(light) ? base.l : clamp01(light),
  };
}

export function accentVars(hue: number, theme: AppTheme, sat?: number | null, light?: number | null): AccentVars {
  const { s, l } = accentSL(theme, sat, light);
  return {
    primary: rgbTriple(hslToRgb(hue / 360, s, l)),
    hover: rgbTriple(hslToRgb(hue / 360, s, Math.min(0.92, l + 0.08))),
  };
}

/** 当前色在给定主题下真正写进 --primary 的十六进制色。 */
export function accentHex(hue: number, theme: AppTheme, sat?: number | null, light?: number | null): string {
  const { s, l } = accentSL(theme, sat, light);
  return toHex(hslToRgb(hue / 360, s, l));
}

/** 同一色相下的深浅色阶，左浅右深。 */
export const ACCENT_SHADES: { s: number; l: number }[] = [
  { s: 0.30, l: 0.88 },
  { s: 0.34, l: 0.78 },
  { s: 0.38, l: 0.68 },
  { s: 0.40, l: 0.58 },
  { s: 0.42, l: 0.48 },
  { s: 0.44, l: 0.38 },
  { s: 0.46, l: 0.30 },
  { s: 0.48, l: 0.24 },
  { s: 0.50, l: 0.18 },
  { s: 0.52, l: 0.13 },
  { s: 0.54, l: 0.09 },
];

/** WCAG 白字对比度；过亮的主题色给提示。 */
export function contrastWithWhite(hex: string): number {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  if (!m) return 21;
  const n = parseInt(m[1], 16);
  const channels = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => {
    const c = v / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  const lum = 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
  return 1.05 / (lum + 0.05);
}
