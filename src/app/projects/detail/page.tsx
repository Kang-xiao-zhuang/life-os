"use client";

import { Suspense, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { PageHeader } from "@/components/common/page-header";
import { SectionCard, EmptyState } from "@/components/common/section-card";
import { TaskRow } from "@/components/common/task-row";
import { TaskEditSheet } from "@/components/common/task-edit-sheet";
import { ListChecks } from "lucide-react";

function DetailContent() {
  const id = useSearchParams().get("id") ?? "";
  const { t } = useT();
  const project = useAppStore((s) => s.projects.find((p) => p.id === id));
  const tasks = useAppStore((s) => s.tasks);
  const hydrated = useAppStore((s) => s.hydrated);
  const [sheet, setSheet] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const open = useMemo(() => tasks.filter((t) => t.projectId === id && !t.done), [tasks, id]);
  const done = useMemo(() => tasks.filter((t) => t.projectId === id && t.done), [tasks, id]);

  if (!hydrated) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;
  if (!project) return <><PageHeader title={t("project.detail")} back /><main className="px-5 pt-6 text-sm text-text-secondary">{t("common.empty")}</main></>;

  return (
    <>
      <PageHeader title={`${project.emoji ? project.emoji + " " : ""}${project.name}`} back />
      <main className="space-y-4 px-5 pt-5">
        <SectionCard title={t("project.open")}>
          {open.length ? <div className="divide-y divide-border/60">{open.map((task) => <TaskRow key={task.id} task={task} onEdit={() => setEditingId(task.id)} />)}</div>
            : <EmptyState icon={<ListChecks />}>{t("project.empty")}</EmptyState>}
        </SectionCard>
        {done.length > 0 && (
          <SectionCard title={t("project.done")} quiet>
            <div className="divide-y divide-border/60">{done.slice(0, 20).map((task) => <TaskRow key={task.id} task={task} onEdit={() => setEditingId(task.id)} />)}</div>
          </SectionCard>
        )}

        <button onClick={() => { setEditingId(null); setSheet(true); }} aria-label={t("project.newTask")}
          className="fixed bottom-24 right-[max(1rem,calc(50%-224px+1rem))] z-30 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-2xl text-background shadow-lg transition-transform active:scale-95">+</button>
        <TaskEditSheet open={sheet} onClose={() => setSheet(false)} defaultProjectId={id} />
        {editingId && <TaskEditSheet open onClose={() => setEditingId(null)} editing={tasks.find((t) => t.id === editingId)} key={editingId} />}
      </main>
    </>
  );
}

export default function ProjectDetailPage() {
  return <Suspense fallback={<div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>}><DetailContent /></Suspense>;
}
