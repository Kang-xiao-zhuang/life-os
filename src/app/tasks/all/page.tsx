"use client";

import { useMemo, useState } from "react";
import { ArrowRight, CheckCircle2 } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { useToday } from "@/lib/use-today";
import { PageHeader } from "@/components/common/page-header";
import { SectionCard, EmptyState } from "@/components/common/section-card";
import { TaskRow } from "@/components/common/task-row";
import type { Task } from "@/types";

export default function AllTasksPage() {
  const { t } = useT();
  const todayStr = useToday();
  const tasks = useAppStore((s) => s.tasks);
  const projects = useAppStore((s) => s.projects);
  const rescheduleOverdue = useAppStore((s) => s.rescheduleOverdue);
  const undoReschedule = useAppStore((s) => s.undoReschedule);
  const hydrated = useAppStore((s) => s.hydrated);
  const [moved, setMoved] = useState<ReturnType<typeof rescheduleOverdue> | null>(null);

  const groups = useMemo(() => {
    const open = tasks.filter((t) => !t.done);
    const pLabel = (id: string | null) => {
      if (!id) return null;
      const p = projects.find((x) => x.id === id);
      return p ? `${p.emoji ? p.emoji + " " : ""}${p.name}` : null;
    };
    const by = (pred: (t: Task) => boolean) => open.filter(pred).sort((a, b) => (a.dueDate ?? "9999").localeCompare(b.dueDate ?? "9999"));
    return {
      overdue: by((t) => t.dueDate !== null && t.dueDate < todayStr),
      today: by((t) => t.dueDate === todayStr),
      later: by((t) => t.dueDate !== null && t.dueDate > todayStr),
      noDate: by((t) => t.dueDate === null),
      pLabel,
    };
  }, [tasks, projects, todayStr]);

  if (!hydrated) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;

  const rows = (list: Task[]) => <div className="divide-y divide-border/60">{list.map((task) => <TaskRow key={task.id} task={task} projectLabel={groups.pLabel(task.projectId)} />)}</div>;

  return (
    <>
      <PageHeader title={t("allTasks.title")} back />
      <main className="space-y-4 px-5 pt-5">
        {tasks.every((t) => t.done) ? (
          <SectionCard><EmptyState icon={<CheckCircle2 />}>{t("allTasks.empty")}</EmptyState></SectionCard>
        ) : (
          <>
            {groups.overdue.length > 0 && (
              <SectionCard title={t("allTasks.overdue")}>
                {rows(groups.overdue)}
                <button onClick={() => {
                  const items = rescheduleOverdue(todayStr);
                  setMoved(items);
                  setTimeout(() => setMoved(null), 4000);
                }} className="mt-3 flex items-center gap-1 text-sm font-medium text-primary">
                  {t("home.moveOverdue", { n: groups.overdue.length })} <ArrowRight size={14} />
                </button>
              </SectionCard>
            )}
            {groups.today.length > 0 && <SectionCard title={t("allTasks.today")}>{rows(groups.today)}</SectionCard>}
            {groups.later.length > 0 && <SectionCard title={t("allTasks.later")} quiet>{rows(groups.later)}</SectionCard>}
            {groups.noDate.length > 0 && <SectionCard title={t("allTasks.noDate")} quiet>{rows(groups.noDate)}</SectionCard>}
          </>
        )}

        {moved && (
          <div className="animate-fade-up fixed bottom-24 left-1/2 z-40 flex -translate-x-1/2 items-center gap-4 rounded-full bg-text-primary px-5 py-3 text-sm text-background shadow-lg">
            <span>{t("home.moved")}</span>
            <button onClick={() => { undoReschedule(moved); setMoved(null); }} className="font-semibold text-tint-habit">{t("common.undo")}</button>
          </div>
        )}
      </main>
    </>
  );
}
