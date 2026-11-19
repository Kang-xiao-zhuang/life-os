"use client";

import { Check, Flame } from "lucide-react";
import type { HabitToday } from "@/utils/business";
import { useAppStore } from "@/store/app.store";
import { cn } from "@/lib/cn";

export function HabitRow({ item, onLongPress }: { item: HabitToday; onLongPress?: () => void }) {
  const toggle = useAppStore((s) => s.toggleHabitCheck);
  let timer: ReturnType<typeof setTimeout> | null = null;

  const handleClick = () => { if (timer) { clearTimeout(timer); timer = null; return; } toggle(item.habit.id); };
  const handleDown = () => { if (!onLongPress) return; timer = setTimeout(() => { onLongPress(); timer = null; }, 500); };
  const handleUp = () => { if (timer) { clearTimeout(timer); timer = null; } };

  return (
    <div
      onClick={handleClick}
      onMouseDown={handleDown} onMouseUp={handleUp} onMouseLeave={handleUp}
      onTouchStart={handleDown} onTouchEnd={handleUp}
      className="flex items-center gap-2.5 rounded-xl px-1.5 py-2 transition-colors active:bg-border/30"
    >
      <button aria-label="打卡" className="flex h-6 w-6 shrink-0 items-center justify-center transition-transform active:scale-90">
        <span className={cn("flex h-[18px] w-[18px] items-center justify-center rounded-full border-2 transition-colors",
          item.checkedToday ? "border-tint-habit bg-tint-habit text-background" : "border-border")}>
          {item.checkedToday && <Check size={12} strokeWidth={3} />}
        </span>
      </button>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5">
          {item.habit.emoji && <span className="text-base">{item.habit.emoji}</span>}
          <span className={cn("truncate text-[15px]", item.checkedToday ? "text-text-primary" : "text-text-secondary")}>{item.habit.name}</span>
        </div>
        <div className="mt-0.5 flex items-center gap-1 text-[11px] text-text-secondary">
          {item.streak > 0 && <><Flame size={11} className="text-tint-habit" />连续 {item.streak} 天</>}
          {item.streak === 0 && <span>{item.week.filter(Boolean).length}/7 本周</span>}
        </div>
      </div>
      <div className="flex shrink-0 gap-[3px]">
        {item.week.map((done, i) => (
          <span key={i} className={cn("h-2 w-2 rounded-full transition-colors", done ? "bg-tint-habit" : "bg-border")} />
        ))}
      </div>
    </div>
  );
}
