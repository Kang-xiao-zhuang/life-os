import { describe, it, expect } from "vitest";
import {
  ACCENT_SHADES,
  DEFAULT_ACCENT_HEX,
  DEFAULT_ACCENT_HUE,
  accentHex,
  accentVars,
  contrastWithWhite,
  hslToHsv,
  hslToRgb,
  hsvToHsl,
} from "@/lib/accent";

describe("hslToRgb", () => {
  it("maps primary hues correctly", () => {
    expect(hslToRgb(0, 1, 0.5)).toEqual({ r: 255, g: 0, b: 0 });
    expect(hslToRgb(1 / 3, 1, 0.5)).toEqual({ r: 0, g: 255, b: 0 });
    expect(hslToRgb(2 / 3, 1, 0.5)).toEqual({ r: 0, g: 0, b: 255 });
  });
  it("returns gray when saturation is zero", () => {
    expect(hslToRgb(0.5, 0, 0.5)).toEqual({ r: 128, g: 128, b: 128 });
  });
});

describe("HSV/HSL 互转", () => {
  it("round trips", () => {
    for (const [s, l] of [[0.42, 0.32], [0.8, 0.6], [0.1, 0.88]]) {
      const hsv = hslToHsv(s, l);
      const back = hsvToHsl(hsv.s, hsv.v);
      expect(back.s).toBeCloseTo(s, 5);
      expect(back.l).toBeCloseTo(l, 5);
    }
  });
});

describe("accentVars", () => {
  it("produces valid RGB triplets in both themes", () => {
    for (const theme of ["light", "dark"] as const) {
      const v = accentVars(210, theme);
      expect(v.primary).toMatch(/^\d{1,3} \d{1,3} \d{1,3}$/);
      expect(v.hover).toMatch(/^\d{1,3} \d{1,3} \d{1,3}$/);
      for (const part of [...v.primary.split(" "), ...v.hover.split(" ")]) {
        expect(Number(part)).toBeGreaterThanOrEqual(0);
        expect(Number(part)).toBeLessThanOrEqual(255);
      }
    }
  });
  it("dark theme is lighter than light theme", () => {
    const sum = (s: string) => s.split(" ").reduce((a, b) => a + Number(b), 0);
    expect(sum(accentVars(150, "dark").primary)).toBeGreaterThan(sum(accentVars(150, "light").primary));
  });
  it("hex feedback matches the actual --primary value", () => {
    const varsToHex = (hue: number, theme: "light" | "dark") =>
      `#${accentVars(hue, theme).primary.split(" ").map((n) => Number(n).toString(16).padStart(2, "0")).join("")}`;
    for (const theme of ["light", "dark"] as const) {
      for (const hue of [0, 82, 210, 359]) expect(accentHex(hue, theme)).toBe(varsToHex(hue, theme));
    }
  });
});

describe("预设与对比度", () => {
  it("默认色相与色阶存在", () => {
    expect(DEFAULT_ACCENT_HUE).toBe(82);
    expect(DEFAULT_ACCENT_HEX.light).toBe("#556B2F");
    expect(ACCENT_SHADES).toHaveLength(11);
  });
  it("white text on default olive is readable", () => {
    expect(contrastWithWhite(DEFAULT_ACCENT_HEX.light)).toBeGreaterThan(3);
  });
});
