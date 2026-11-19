"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Settings, ChevronRight, Inbox, PenLine, ArrowRight, Sparkles } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT, dailyQuote } from "@/lib/i18n";
import type { RescheduledTask } from "@/types";
import { useToday } from "@/lib/use-today";
import { dateKey, formatDate, addDays, parseDate, yearProgress } from "@/utils/date";
import { activityDays, dashboardData } from "@/utils/business";
import { SectionCard, EmptyHint } from "@/components/common/section-card";
import { TaskRow } from "@/components/common/task-row";
import { HabitRow } from "@/components/common/habit-row";
import { TaskEditSheet } from "@/components/common/task-edit-sheet";
import { ActivityChart } from "@/components/common/activity-chart";
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

  const year = useMemo(() => (todayStr ? yearProgress(parseDate(todayStr)) : null), [todayStr]);
  const activity = useMemo(() => (todayStr ? activityDays(tasks, checks, todayStr, 30) : []), [tasks, checks, todayStr]);
  const activityTotal = activity.reduce((sum, p) => sum + p.count, 0);

  if (!hydrated || !d) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;

  const projectLabel = (id: string | null) => {
    if (!id) return null;
    const p = projects.find((x) => x.id === id);
    return p ? `${p.emoji ? p.emoji + " " : ""}${p.name}` : null;
  };
  const quote = dailyQuote(lang, todayStr);
  const todayDate = parseDate(todayStr);

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

      {/* 年度进度：日期 / 第几天 / 还剩几天 + 今日三格概览 */}
      {year && (
        <section className="mt-4 animate-fade-up overflow-hidden rounded-card border border-primary/15 bg-primary/[0.05]">
          <div className="flex items-stretch">
            <div className="min-w-0 flex-1 px-3 py-2.5">
              <p className="truncate text-base font-bold leading-tight text-text-primary">
                {t("home.yearDate", lang === "zh"
                  ? { m: todayDate.getMonth() + 1, d: todayDate.getDate() }
                  : { mon: todayDate.toLocaleString("en", { month: "short" }), d: todayDate.getDate() })}
              </p>
              <p className="mt-0.5 truncate text-[11px] text-text-secondary">
                {lang === "zh" ? `${year.year}年 · ` : `${year.year} · `}
                {todayDate.toLocaleDateString(lang === "zh" ? "zh-CN" : "en-US", { weekday: "short" })}
              </p>
            </div>
            <div className="my-2.5 w-px shrink-0 bg-primary/15" />
            <div className="min-w-0 flex-1 px-3 py-2.5">
              <p className="truncate text-base font-bold leading-tight text-primary">{t("home.yearDay", { n: year.dayOfYear })}</p>
              <p className="mt-0.5 truncate text-[11px] text-text-secondary">{t("home.yearDaySub")}</p>
            </div>
            <div className="my-2.5 w-px shrink-0 bg-primary/15" />
            <div className="min-w-0 flex-1 px-3 py-2.5">
              <p className="truncate text-base font-bold leading-tight text-text-primary">{t("home.yearLeft", { n: year.daysLeft })}</p>
              <p className="mt-0.5 truncate text-[11px] text-text-secondary">{t("home.yearLeftSub")}</p>
            </div>
          </div>
          <div className="grid grid-cols-3 border-t border-primary/15">
            {[
              { label: t("home.statTasks"), value: String(d.todayDone) },
              { label: t("home.statHabits"), value: `${d.todayHabits}/${d.habits.length}` },
              { label: t("home.statJournal"), value: d.hasJournal ? t("home.statJournalDone") : "—" },
            ].map((stat, i) => (
              <div key={stat.label} className={`px-3 py-2 ${i < 2 ? "border-r border-primary/15" : ""}`}>
                <p className="text-sm font-semibold tabular-nums text-text-primary">{stat.value}</p>
                <p className="mt-0.5 truncate text-[10px] text-text-secondary">{stat.label}</p>
              </div>
            ))}
          </div>
          <div className="h-1 w-full bg-primary/10">
            <div className="h-full rounded-r-full bg-primary" style={{ width: `${(year.dayOfYear / year.totalDays) * 100}%` }} />
          </div>
        </section>
      )}

      {/* 第二栏：快速记录 + 今日复盘 */}
      <div className="mt-4 grid grid-cols-2 gap-3">
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

      {/* MIT —— 唯一强调区，直接坐在背景上，不是卡片 */}
      <section className="mt-6">
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

      {/* 近 30 天活跃 —— 放在最下，不抢上面的行动区 */}
      <SectionCard
        title={t("home.activity")}
        className="mt-5 animate-fade-up"
        trailing={t("home.activityTotal", { n: activityTotal })}
      >
        <ActivityChart
          data={activity}
          unitText={t("home.activity")}
          emptyText={t("home.activityEmpty")}
          emptySub={t("home.activityEmptySub")}
        />
      </SectionCard>

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
