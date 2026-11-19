"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Settings, ChevronRight, Inbox, PenLine, ArrowRight, Sparkles } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT, dailyQuote } from "@/lib/i18n";
import type { RescheduledTask } from "@/types";
import { useToday } from "@/lib/use-today";
import { dateKey, formatDate, addDays } from "@/utils/date";
import { dashboardData } from "@/utils/business";
import { SectionCard, EmptyHint } from "@/components/common/section-card";
import { TaskRow } from "@/components/common/task-row";
import { HabitRow } from "@/components/common/habit-row";
import { TaskEditSheet } from "@/components/common/task-edit-sheet";
import { toPlainText } from "@/components/common/markdown";

export default function HomePage() {
  const { t, lang } = useT();
  const todayStr = useToday();
  const tasks = useAppStore((s) => s.tasks);
  const habits = useAppStore((s) => s.habits);
  const checks = useAppStore((s) => s.habitChecks);
  const journals = useAppStore((s) => s.journals);
  const captures = useAppStore((s) => s.captures);
  const projects = useAppStore((s) => s.projects);
  const rescheduleOverdue = useAppStore((s) => s.rescheduleOverdue);
  const undoReschedule = useAppStore((s) => s.undoReschedule);
  const hydrated = useAppStore((s) => s.hydrated);

  const [editing, setEditing] = useState(false);
  const [moved, setMoved] = useState<RescheduledTask[] | null>(null);
  const [moveCount, setMoveCount] = useState(0);

  const d = useMemo(() => {
    if (!todayStr) return null;
    return dashboardData(tasks, habits, checks, journals, captures, todayStr);
  }, [tasks, habits, checks, journals, captures, todayStr]);

  if (!hydrated || !d) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;

  const projectLabel = (id: string | null) => {
    if (!id) return null;
    const p = projects.find((x) => x.id === id);
    return p ? `${p.emoji ? p.emoji + " " : ""}${p.name}` : null;
  };
  const quote = dailyQuote(lang, todayStr);

  const onMoveOverdue = () => {
    const movedItems = rescheduleOverdue(todayStr);
    setMoveCount(movedItems.length);
    setMoved(movedItems);
    setTimeout(() => { setMoved(null); setMoveCount(0); }, 4000);
  };

  return (
    <main className="px-5 pt-6">
      {/* 顶栏 */}
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-tint-task">{t("home.today")}</p>
          <h1 className="mt-0.5 text-3xl font-bold leading-tight text-text-primary">{formatDate(todayStr, lang)}</h1>
        </div>
        <Link href="/settings" aria-label={t("home.settings")} className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-text-secondary hover:bg-border/50">
          <Settings size={21} />
        </Link>
      </div>
      <p className="mt-2 flex items-center gap-1.5 text-[13px] leading-relaxed text-text-secondary">
        <Sparkles size={14} className="shrink-0 text-tint-journal" />{quote}
      </p>

      {/* MIT —— 唯一强调区，直接坐在背景上，不是卡片 */}
      <section className="mt-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="rounded-md bg-primary/12 px-2 py-0.5 text-xs font-semibold text-primary">MIT</span>
            <span className="text-xs font-medium text-text-secondary">{t("home.mit")}</span>
          </div>
          <span className="text-xs text-text-secondary">{d.mit.length}/2</span>
        </div>
        <div className="mt-1.5 space-y-0.5">
          {d.mit.length ? d.mit.map((task) => (
            <TaskRow key={task.id} task={task} large projectLabel={projectLabel(task.projectId)} onEdit={() => setEditing(true)} />
          )) : (
            <div className="py-2">
              {d.carriedMit && (
                <p className="mb-1.5 text-xs text-text-secondary">{t("home.mitCarried")}</p>
              )}
              <EmptyHint>{d.carriedMit ? toPlainText(d.carriedMit) : t("home.mitEmpty")}</EmptyHint>
            </div>
          )}
        </div>
        <div className="mt-2 border-b border-border" />
      </section>

      {/* 今日任务 */}
      <SectionCard
        title={t("home.tasksToday")}
        className="mt-6"
        trailing={d.overdue.length ? <span className="text-danger">{t("home.overdue", { n: d.overdue.length })}</span> : undefined}
      >
        {[...d.overdue, ...d.dueToday].length ? (
          <div className="divide-y divide-border/60">
            {[...d.overdue, ...d.dueToday].map((task) => (
              <TaskRow key={task.id} task={task} projectLabel={projectLabel(task.projectId)} onEdit={() => setEditing(true)} />
            ))}
          </div>
        ) : <EmptyHint>{t("home.tasksEmpty")}</EmptyHint>}
        {d.overdue.length > 0 && (
          <button onClick={onMoveOverdue} className="mt-3 flex items-center gap-1 text-sm font-medium text-primary">
            {t("home.moveOverdue", { n: d.overdue.length })} <ArrowRight size={14} />
          </button>
        )}
      </SectionCard>

      {/* 今日习惯 */}
      <Link href="/habits" className="mt-5 block">
        <SectionCard title={t("home.habits")} quiet trailing={`${t("home.habitsDone", { done: d.habits.filter((h) => h.checkedToday).length, total: d.habits.length })}`}>
          {d.habits.length ? (
            <div className="space-y-1">
              {d.habits.slice(0, 4).map((item) => <HabitRow key={item.habit.id} item={item} />)}
              {d.habits.length > 4 && <EmptyHint className="mt-1">+{d.habits.length - 4}</EmptyHint>}
            </div>
          ) : <EmptyHint>{t("home.habitsEmpty")}</EmptyHint>}
        </SectionCard>
      </Link>

      {/* 记录 + 复盘入口 */}
      <div className="mt-5 grid grid-cols-2 gap-3">
        <Link href="/capture">
          <SectionCard quiet className="h-full">
            <div className="flex h-full flex-col justify-between gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-tint-habit/12 text-tint-habit"><Inbox size={18} /></div>
              <div>
                <p className="text-sm font-medium text-text-primary">{t("home.quickCapture")}</p>
                <p className="mt-0.5 line-clamp-2 text-xs text-text-secondary">{t("home.quickCaptureSub")}</p>
              </div>
            </div>
          </SectionCard>
        </Link>
        <Link href="/journal">
          <SectionCard quiet className="h-full">
            <div className="flex h-full flex-col justify-between gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-tint-journal/12 text-tint-journal"><PenLine size={18} /></div>
              <div>
                <p className="flex items-center gap-1.5 text-sm font-medium text-text-primary">
                  {t("home.journal")}{d.journal && <span className="text-[10px] text-tint-habit">✓</span>}
                </p>
                <p className="mt-0.5 line-clamp-2 text-xs text-text-secondary">
                  {d.journal ? toPlainText(d.journal.win || d.journal.done).slice(0, 40) : t("home.journalEmpty")}
                </p>
              </div>
            </div>
          </SectionCard>
        </Link>
      </div>

      {/* 逾期挪期撤销提示 */}
      {moved && (
        <div className="animate-fade-up fixed bottom-24 left-1/2 z-40 flex -translate-x-1/2 items-center gap-4 rounded-full bg-text-primary px-5 py-3 text-sm text-background shadow-lg">
          <span>{t("home.moved")} {moveCount}</span>
          <button onClick={() => { undoReschedule(moved); setMoved(null); }} className="font-semibold text-tint-habit">{t("common.undo")}</button>
        </div>
      )}

      {/* FAB 新建任务 */}
      <button onClick={() => setEditing(true)} aria-label={t("task.new")}
        className="fixed bottom-24 right-[max(1rem,calc(50%-224px+1rem))] z-30 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-background shadow-lg transition-transform active:scale-95">
        <span className="text-2xl leading-none">+</span>
      </button>

      <TaskEditSheet open={editing} onClose={() => setEditing(false)} />
    </main>
  );
}
