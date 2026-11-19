"use client";

import { useId } from "react";
import { useAccentColors } from "@/lib/use-accent";
import type { ActivityPoint } from "@/utils/business";

interface ActivityChartProps {
  data: ActivityPoint[];
  emptyText: string;
  emptySub: string;
  unitText: string;
  height?: number;
}

/**
 * 轻量活跃曲线（自绘 SVG，不引入图表库）。
 * 颜色全部用真实 rgb 值：Android WebView 对 SVG 属性里的 var() 支持不完整。
 */
export function ActivityChart({ data, emptyText, emptySub, unitText, height = 150 }: ActivityChartProps) {
  const accent = useAccentColors();
  const rawId = useId().replace(/:/g, "");
  const gradientId = `lifeos-activity-${rawId}`;
  const total = data.reduce((s, d) => s + d.count, 0);
  const peak = data.reduce((m, d) => Math.max(m, d.count), 0);

  if (total === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border/70 text-center"
        style={{ height }}>
        <div className="flex items-end gap-1 opacity-45">
          {[10, 16, 8, 20, 12].map((h, i) => (
            <span key={i} className="w-1.5 rounded-full bg-text-secondary" style={{ height: h }} />
          ))}
        </div>
        <p className="mt-2.5 text-[11px] font-medium text-text-primary">{emptyText}</p>
        <p className="mt-0.5 text-[10px] text-text-secondary">{emptySub}</p>
      </div>
    );
  }

  const W = 300;
  const H = 100;
  const padX = 3;
  const padTop = 6;
  const padBottom = 4;
  const innerH = H - padTop - padBottom;
  const max = Math.max(3, peak + (peak <= 5 ? 1 : Math.ceil(peak * 0.15)));
  const pts = data.map((d, i) => ({
    x: data.length === 1 ? W / 2 : padX + (i * (W - padX * 2)) / (data.length - 1),
    y: padTop + innerH * (1 - d.count / max),
  }));
  // 相邻点用水平切线平滑连接：比折线柔和，又不会像单调三次插值那样 overshoot。
  let line = `M ${pts[0].x.toFixed(2)} ${pts[0].y.toFixed(2)}`;
  for (let i = 1; i < pts.length; i++) {
    const prev = pts[i - 1];
    const cur = pts[i];
    const mid = (prev.x + cur.x) / 2;
    line += ` C ${mid.toFixed(2)} ${prev.y.toFixed(2)}, ${mid.toFixed(2)} ${cur.y.toFixed(2)}, ${cur.x.toFixed(2)} ${cur.y.toFixed(2)}`;
  }
  const baseY = H - padBottom;
  const area = `${line} L ${pts[pts.length - 1].x.toFixed(2)} ${baseY} L ${pts[0].x.toFixed(2)} ${baseY} Z`;
  const last = pts[pts.length - 1];
  const hasLast = data[data.length - 1].count > 0;

  return (
    <div style={{ height }}>
      <svg width="100%" height={H} viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" aria-label={unitText} role="img">
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={accent.css} stopOpacity={0.34} />
            <stop offset="60%" stopColor={accent.css} stopOpacity={0.12} />
            <stop offset="100%" stopColor={accent.css} stopOpacity={0.02} />
          </linearGradient>
        </defs>
        {[0, 0.5, 1].map((r) => {
          const y = padTop + innerH * r;
          return <line key={r} x1={0} x2={W} y1={y} y2={y} stroke={accent.grid} strokeWidth={1} strokeDasharray="2 5" />;
        })}
        <path d={area} fill={`url(#${gradientId})`} />
        <path d={line} fill="none" stroke={accent.css} strokeWidth={2.2} strokeLinecap="round" strokeLinejoin="round" />
        {data.map((d, i) => (
          <circle key={d.date} cx={pts[i].x} cy={pts[i].y} r={9} fill="transparent">
            <title>{`${d.date} · ${d.count}`}</title>
          </circle>
        ))}
        {hasLast && (
          <g>
            <circle cx={last.x} cy={last.y} r={7} fill={accent.css} opacity={0.16} />
            <circle cx={last.x} cy={last.y} r={3.1} fill={accent.css} stroke={accent.surface} strokeWidth={1.6} />
          </g>
        )}
      </svg>
      <div className="mt-1 flex justify-between text-[10px] tabular-nums text-text-secondary">
        <span>{data[0].date.replace("-", "/")}</span>
        <span>{data[data.length - 1].date.replace("-", "/")}</span>
      </div>
    </div>
  );
}
