import type { Config } from "tailwindcss";

// Design tokens mirror docs/04-DESIGN-SYSTEM.md — the single source of visual truth.
const config: Config = {
  darkMode: "class",
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // referenced via CSS variables so dark mode can re-map them
        primary: "rgb(var(--primary) / <alpha-value>)",
        "primary-hover": "rgb(var(--primary-hover) / <alpha-value>)",
        background: "rgb(var(--background) / <alpha-value>)",
        surface: "rgb(var(--surface) / <alpha-value>)",
        "surface-quiet": "rgb(var(--surface-quiet) / <alpha-value>)",
        border: "rgb(var(--border) / <alpha-value>)",
        "text-primary": "rgb(var(--text-primary) / <alpha-value>)",
        "text-secondary": "rgb(var(--text-secondary) / <alpha-value>)",
        success: "rgb(var(--success) / <alpha-value>)",
        warning: "rgb(var(--warning) / <alpha-value>)",
        danger: "rgb(var(--danger) / <alpha-value>)",
        "tint-task": "rgb(var(--tint-task) / <alpha-value>)",
        "tint-project": "rgb(var(--tint-project) / <alpha-value>)",
        "tint-habit": "rgb(var(--tint-habit) / <alpha-value>)",
        "tint-journal": "rgb(var(--tint-journal) / <alpha-value>)",
      },
      borderRadius: {
        btn: "16px",
        card: "20px",
        input: "16px",
        modal: "24px",
      },
      fontFamily: {
        sans: ["Inter", "Noto Sans SC", "system-ui", "sans-serif"],
      },
      boxShadow: {
        card: "0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06)",
        hover: "0 6px 18px rgba(0,0,0,0.08)",
        modal: "0 20px 50px rgba(0,0,0,0.18)",
      },
      maxWidth: {
        app: "480px",
      },
    },
  },
  plugins: [],
};

export default config;
