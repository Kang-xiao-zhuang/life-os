"use client";

import { useRef } from "react";
import { Check, Minus, Plus, RotateCcw } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { resolvedTheme } from "@/lib/use-theme";
import {
  ACCENT_SHADES,
  DEFAULT_ACCENT_HEX,
  DEFAULT_ACCENT_HUE,
  accentHex,
  accentSL,
  accentVars,
  contrastWithWhite,
  hslToHsv,
  hsvToHsl,
} from "@/lib/accent";

const clamp01 = (n: number) => Math.max(0, Math.min(1, n));

/** 色相环 + HSV 方形 + 色阶预设；环定色相，方形定饱和度与明暗，色阶给一组安全深浅。 */
export function AccentPicker() {
  const { t } = useT();
  const themeMode = useAppStore((s) => s.settings.themeMode);
  const accentHue = useAppStore((s) => s.settings.accentHue);
  const accentSat = useAppStore((s) => s.settings.accentSat);
  const accentLight = useAppStore((s) => s.settings.accentLight);
  const setAccentColor = useAppStore((s) => s.setAccentColor);
  const ringRef = useRef<HTMLDivElement>(null);
  const squareRef = useRef<HTMLDivElement>(null);
  const ringDragging = useRef(false);
  const squareDragging = useRef(false);

  const theme = resolvedTheme(themeMode);
  const isDefault = accentHue == null;
  const hue = accentHue ?? DEFAULT_ACCENT_HUE;
  const { s: sat, l: light } = accentSL(theme, accentSat, accentLight);
  const hex = (isDefault ? DEFAULT_ACCENT_HEX[theme] : accentHex(hue, theme, accentSat, accentLight)).toUpperCase();
  const { s: svSat, v } = hslToHsv(sat, light);
  const current = `hsl(${hue} ${sat * 100}% ${light * 100}%)`;
  const pureHue = `hsl(${hue} 100% 50%)`;
  const lowContrast = contrastWithWhite(hex) < 3;

  // 色相环只在外圈环带响应；中心区域交给方形面板。
  const pickHue = (clientX: number, clientY: number) => {
    const el = ringRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const dx = clientX - (rect.left + rect.width / 2);
    const dy = clientY - (rect.top + rect.height / 2);
    const dist = Math.hypot(dx, dy) / (rect.width / 2);
    if (dist < 0.75 || dist > 1.06) return;
    const angle = (Math.atan2(dx, -dy) * 180) / Math.PI;
    setAccentColor(Math.round((angle + 360) % 360), accentSat, accentLight);
  };

  // 方形面板：x = 饱和度，y 越往下越暗（HSV），存成 HSL 的 s/l。
  const pickSquare = (clientX: number, clientY: number) => {
    const el = squareRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const x = clamp01((clientX - rect.left) / rect.width);
    const y = clamp01((clientY - rect.top) / rect.height);
    const next = hsvToHsl(x, 1 - y);
    setAccentColor(hue, next.s, next.l);
  };

  const ringRad = (hue * Math.PI) / 180;
  const ringMarker = {
    left: `${50 + 45.5 * Math.sin(ringRad)}%`,
    top: `${50 - 45.5 * Math.cos(ringRad)}%`,
  };
  const squareMarker = { left: `${svSat * 100}%`, top: `${(1 - v) * 100}%` };
  const shadeActive = (shade: { s: number; l: number }) =>
    accentSat != null &&
    accentLight != null &&
    Math.abs(accentSat - shade.s) < 0.005 &&
    Math.abs(accentLight - shade.l) < 0.005;

  return (
    <div className="px-0 pb-1 pt-2">
      <p className="text-xs leading-relaxed text-text-secondary">{t("settings.accentHint")}</p>
      <div className="mt-3 flex flex-col items-center gap-3">
        <div className="relative h-48 w-48 shrink-0">
          <div
            className="pointer-events-none absolute inset-0 rounded-full"
            style={{
              background:
                "conic-gradient(#ff0000 0deg, #ffff00 60deg, #00ff00 120deg, #00ffff 180deg, #0000ff 240deg, #ff00ff 300deg, #ff0000 360deg)",
            }}
          />
          <div
            ref={ringRef}
            className="absolute inset-0 cursor-crosshair touch-none rounded-full"
            role="slider"
            aria-label={t("settings.accentHue")}
            aria-valuemin={0}
            aria-valuemax={360}
            aria-valuenow={hue}
            aria-valuetext={`${hue}° ${hex}`}
            tabIndex={0}
            onKeyDown={(e) => {
              const step = e.shiftKey ? 10 : 1;
              if (e.key === "ArrowRight" || e.key === "ArrowUp") {
                e.preventDefault();
                setAccentColor((hue + step) % 360, accentSat, accentLight);
              }
              if (e.key === "ArrowLeft" || e.key === "ArrowDown") {
                e.preventDefault();
                setAccentColor((hue - step + 360) % 360, accentSat, accentLight);
              }
            }}
            onPointerDown={(e) => {
              ringDragging.current = true;
              e.currentTarget.setPointerCapture(e.pointerId);
              pickHue(e.clientX, e.clientY);
            }}
            onPointerMove={(e) => {
              if (ringDragging.current) pickHue(e.clientX, e.clientY);
            }}
            onPointerUp={(e) => {
              ringDragging.current = false;
              e.currentTarget.releasePointerCapture(e.pointerId);
            }}
            onPointerCancel={() => { ringDragging.current = false; }}
          />
          <div className="pointer-events-none absolute inset-[9%] rounded-full bg-surface" />
          <div
            ref={squareRef}
            className="absolute inset-[19%] cursor-crosshair touch-none overflow-hidden rounded-xl shadow-card"
            style={{ background: `linear-gradient(to top, #000, rgba(0,0,0,0)), linear-gradient(to right, #fff, ${pureHue})` }}
            onPointerDown={(e) => {
              squareDragging.current = true;
              e.currentTarget.setPointerCapture(e.pointerId);
              pickSquare(e.clientX, e.clientY);
            }}
            onPointerMove={(e) => {
              if (squareDragging.current) pickSquare(e.clientX, e.clientY);
            }}
            onPointerUp={(e) => {
              squareDragging.current = false;
              e.currentTarget.releasePointerCapture(e.pointerId);
            }}
            onPointerCancel={() => { squareDragging.current = false; }}
          >
            <div
              className="pointer-events-none absolute h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full border-[3px] border-white shadow-card"
              style={{ ...squareMarker, backgroundColor: current }}
            />
          </div>
          <div
            className="pointer-events-none absolute h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full border-[3px] border-white shadow-card"
            style={{ ...ringMarker, backgroundColor: pureHue }}
          />
        </div>

        <div className="flex w-full items-center justify-center gap-2">
          <span className="font-mono text-[11px] text-text-secondary">H</span>
          <span className="min-w-[34px] font-mono text-sm font-semibold tabular-nums text-text-primary">{hue}°</span>
          <button type="button" aria-label="-1°" onClick={() => setAccentColor((hue - 1 + 360) % 360, accentSat, accentLight)}
            className="flex h-6 w-6 items-center justify-center rounded-full border border-border text-text-secondary active:opacity-60">
            <Minus size={12} />
          </button>
          <button type="button" aria-label="+1°" onClick={() => setAccentColor((hue + 1) % 360, accentSat, accentLight)}
            className="flex h-6 w-6 items-center justify-center rounded-full border border-border text-text-secondary active:opacity-60">
            <Plus size={12} />
          </button>
          <span className="ml-2 font-mono text-xs tabular-nums text-text-primary">{hex}</span>
        </div>
        <div className="-mt-2 font-mono text-[11px] tabular-nums text-text-secondary">
          S {Math.round(svSat * 100)}% · V {Math.round(v * 100)}%
        </div>
        {lowContrast && <p className="-mt-1 text-center text-[11px] text-text-secondary">{t("settings.accentLowContrast")}</p>}

        <div className="grid w-full grid-cols-7 gap-2">
          {ACCENT_SHADES.map((shade, i) => {
            const c = `rgb(${accentVars(hue, "light", shade.s, shade.l).primary})`;
            const active = shadeActive(shade);
            return (
              <button key={i} type="button" aria-label={`shade-${i + 1}`} onClick={() => setAccentColor(hue, shade.s, shade.l)}
                className="flex aspect-square items-center justify-center rounded-lg border-2 transition-transform active:scale-90"
                style={{ backgroundColor: c, borderColor: active ? "rgb(var(--text-primary))" : "transparent" }}>
                {active && <Check size={14} color={shade.l > 0.6 ? "#000" : "#fff"} strokeWidth={3} />}
              </button>
            );
          })}
        </div>

        <button type="button" onClick={() => setAccentColor(null)}
          className="flex items-center gap-1.5 self-center rounded-full border border-border px-3 py-1.5 text-xs text-text-secondary active:opacity-70">
          <RotateCcw size={13} />
          {t("settings.accentDefault")}
        </button>
      </div>
    </div>
  );
}
