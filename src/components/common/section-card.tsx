"use client";

import { cn } from "@/lib/cn";

interface SectionCardProps {
  title?: string;
  trailing?: React.ReactNode;
  quiet?: boolean;
  onClick?: () => void;
  children: React.ReactNode;
  className?: string;
}

/** 全 App 唯一的卡片形制。quiet = 退后一级（更淡的面、更小的标题、更弱的颜色）。 */
export function SectionCard({ title, trailing, quiet, onClick, children, className }: SectionCardProps) {
  const Wrapper = onClick ? "button" : "div";
  return (
    <Wrapper
      onClick={onClick}
      type={onClick ? "button" : undefined}
      className={cn(
        "w-full text-left transition-opacity",
        quiet ? "rounded-card bg-surface-quiet p-4" : "rounded-card border border-border bg-surface p-[18px] shadow-card",
        onClick && "active:opacity-70",
        className,
      )}
    >
      {(title || trailing) && (
        <div className={cn("flex items-center justify-between gap-2", quiet ? "mb-2" : "mb-3.5")}>
          {title && (
            <h2 className={cn(quiet ? "text-sm font-medium text-text-secondary" : "text-[17px] font-semibold text-text-primary")}>
              {title}
            </h2>
          )}
          {trailing && <span className="shrink-0 text-xs font-medium text-text-secondary">{trailing}</span>}
        </div>
      )}
      {children}
    </Wrapper>
  );
}

/** 柔和的单行提示（不是错误）。 */
export function EmptyHint({ children, className }: { children: React.ReactNode; className?: string }) {
  return <p className={cn("text-[13px] leading-relaxed text-text-secondary", className)}>{children}</p>;
}

/** 整屏空状态：图标 + 一行文字。 */
export function EmptyState({ icon, children }: { icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3.5 py-8 text-center">
      <div className="text-text-secondary opacity-30 [&>svg]:h-11 [&>svg]:w-11">{icon}</div>
      <p className="max-w-[240px] text-[13px] leading-relaxed text-text-secondary">{children}</p>
    </div>
  );
}
