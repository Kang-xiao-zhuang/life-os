"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { cn } from "@/lib/cn";
import { shiftMonth, monthDays, formatMonth, today as todayKey } from "@/utils/date";
import { useState } from "react";
import { habitMonth } from "@/utils/business";

export function HabitHeatmap() {
  const { t, lang } = useT();
  const checks = useAppStore((s) => s.habitChecks);
  const habits = useAppStore((s) => s.habits);
  const [ym, setYm] = useState(() => todayKey().slice(0, 7));
  const todayStr = todayKey();
  const currentYm = todayStr.slice(0, 7);
  const activeCount = habits.filter((h) => !h.archived).length;
  const cells = habitMonth(checks, activeCount, monthDays(ym), todayStr);
  // 月历首日前的空格
  const [y, m] = ym.split("-").map(Number);
  const firstDow = (new Date(y, m - 1, 1).getDay() + 6) % 7; // 周一=0

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <button onClick={() => setYm(shiftMonth(ym, -1))} aria-label="上个月" className="flex h-8 w-8 items-center justify-center rounded-lg text-text-secondary hover:bg-border/50"><ChevronLeft size={20} /></button>
        <span className="text-sm font-semibold text-text-primary">{formatMonth(ym, lang)}</span>
        <button onClick={() => setYm(shiftMonth(ym, 1))} disabled={ym >= currentYm} aria-label="下个月"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-text-secondary hover:bg-border/50 disabled:opacity-25"><ChevronRight size={20} /></button>
      </div>

      <div className="mb-1.5 grid grid-cols-7 gap-1">
        {["一", "二", "三", "四", "五", "六", "日"].map((w) => (
          <span key={w} className="text-center text-[10px] font-medium text-text-secondary">{w}</span>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: firstDow }, (_, i) => <div key={`pad-${i}`} />)}
        {cells.map((cell) => {
          const day = Number(cell.date.slice(8));
          const isToday = cell.date === todayStr;
          return (
            <div key={cell.date}
              className={cn("flex aspect-square items-center justify-center rounded-md text-[10px] transition-colors",
                cell.completion === null ? "bg-surface text-text-secondary/40"
                  : cell.completion === 0 ? "bg-border/50 text-text-secondary"
                    : "bg-tint-habit text-background",
                isToday && "ring-2 ring-primary ring-offset-1 ring-offset-surface")}
              style={cell.completion !== null && cell.completion > 0 ? { opacity: 0.35 + 0.65 * cell.completion } : undefined}
              title={`${cell.date} · ${cell.completion === null ? "" : Math.round(cell.completion * 100) + "%"}`}>
              <span className={cn(isToday && "font-bold")}>{day}</span>
            </div>
          );
        })}
      </div>

      <div className="mt-3 flex items-center justify-between text-[11px] text-text-secondary">
        <span>{t("habits.days", { n: cells.filter((c) => c.completion !== null && c.completion > 0).length })}</span>
        <span className="flex items-center gap-1">
          少
          <span className="inline-block h-2.5 w-2.5 rounded-sm bg-border/50" />
          <span className="inline-block h-2.5 w-2.5 rounded-sm bg-tint-habit/50" />
          <span className="inline-block h-2.5 w-2.5 rounded-sm bg-tint-habit" />
          多
        </span>
      </div>
    </div>
  );
}
