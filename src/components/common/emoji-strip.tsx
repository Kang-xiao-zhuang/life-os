"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { EMOJIS } from "@/lib/emoji";
import { useT } from "@/lib/i18n";
import { cn } from "@/lib/cn";

interface Props {
  value: string;
  onChange: (emoji: string) => void;
}

/** 两行横向滚动的图标条，两端箭头翻页；老数据里不在库里的图标放最前，保证仍可选中。 */
export function EmojiStrip({ value, onChange }: Props) {
  const { t } = useT();
  const stripRef = useRef<HTMLDivElement>(null);
  const [edge, setEdge] = useState({ start: true, end: false });
  const icons = useMemo(() => (value && !EMOJIS.includes(value) ? [value, ...EMOJIS] : EMOJIS), [value]);

  const syncEdge = useCallback(() => {
    const el = stripRef.current;
    if (!el) return;
    setEdge({
      start: el.scrollLeft <= 2,
      end: el.scrollLeft + el.clientWidth >= el.scrollWidth - 2,
    });
  }, []);

  // 编辑时把已选图标滚到可视中间；首次打开（无选中）同步一下箭头状态
  useEffect(() => {
    const box = stripRef.current;
    const on = box?.querySelector<HTMLElement>('[data-on="true"]');
    if (box && on) box.scrollLeft = on.offsetLeft - box.clientWidth / 2 + on.clientWidth / 2;
    syncEdge();
  }, [syncEdge, value]);

  const page = (dir: number) => {
    const el = stripRef.current;
    if (!el) return;
    el.scrollBy({ left: dir * Math.max(120, el.clientWidth * 0.85), behavior: "smooth" });
  };

  return (
    <div className="flex items-center gap-1.5">
      <button type="button" aria-label={t("common.prev")} onClick={() => page(-1)} disabled={edge.start}
        className="flex h-9 w-8 shrink-0 items-center justify-center rounded-full border border-border/80 bg-background text-text-secondary transition-opacity active:scale-90 disabled:opacity-30">
        <ChevronLeft size={17} />
      </button>

      <div className="relative min-w-0 flex-1">
        <div ref={stripRef} onScroll={syncEdge} className="no-scrollbar overflow-x-auto">
          <div className="grid w-max grid-flow-col grid-rows-2 gap-2 py-0.5">
            {icons.map((emoji) => (
              <button key={emoji} type="button" data-on={emoji === value} aria-pressed={emoji === value}
                onClick={() => onChange(emoji === value ? "" : emoji)}
                className={cn(
                  "flex h-10 w-10 items-center justify-center rounded-xl border text-xl leading-none transition-transform active:scale-90",
                  emoji === value
                    ? "scale-105 border-primary bg-primary/12 ring-2 ring-primary/60"
                    : "border-border/70 bg-background",
                )}>
                {emoji}
              </button>
            ))}
          </div>
        </div>
        {!edge.start && <div className="pointer-events-none absolute inset-y-0 left-0 w-7 bg-gradient-to-r from-surface to-transparent" />}
        {!edge.end && <div className="pointer-events-none absolute inset-y-0 right-0 w-7 bg-gradient-to-l from-surface to-transparent" />}
      </div>

      <button type="button" aria-label={t("common.next")} onClick={() => page(1)} disabled={edge.end}
        className="flex h-9 w-8 shrink-0 items-center justify-center rounded-full border border-border/80 bg-background text-text-secondary transition-opacity active:scale-90 disabled:opacity-30">
        <ChevronRight size={17} />
      </button>
    </div>
  );
}
