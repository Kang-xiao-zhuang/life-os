"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FolderOpen, ChevronRight, ListTodo, Archive, MoreHorizontal } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { SectionCard, EmptyState, EmptyHint } from "@/components/common/section-card";
import { NameEmojiDialog } from "@/components/common/name-emoji-dialog";
import { TaskRow } from "@/components/common/task-row";
import { TaskEditSheet } from "@/components/common/task-edit-sheet";
import { projectSummaries, sortOpenTasks } from "@/utils/business";
import { useToday } from "@/lib/use-today";
import type { Project } from "@/types";

export default function ProjectsPage() {
  const { t } = useT();
  const todayStr = useToday();
  const router = useRouter();
  const projects = useAppStore((s) => s.projects);
  const tasks = useAppStore((s) => s.tasks);
  const createProject = useAppStore((s) => s.createProject);
  const updateProject = useAppStore((s) => s.updateProject);
  const setArchived = useAppStore((s) => s.setProjectArchived);
  const hydrated = useAppStore((s) => s.hydrated);

  const [showNew, setShowNew] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [taskSheet, setTaskSheet] = useState(false);

  const summaries = useMemo(() => projectSummaries(projects, tasks), [projects, tasks]);
  const archivedCount = projects.filter((p) => p.archived).length;
  const unassigned = useMemo(() => sortOpenTasks(tasks.filter((t) => !t.projectId && !t.done), todayStr), [tasks, todayStr]);
  const openCount = tasks.filter((t) => !t.done).length;

  if (!hydrated) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;

  return (
    <main className="px-5 pt-6">
      <h1 className="text-2xl font-bold text-text-primary">{t("projects.title")}</h1>

      <div className="mt-5 space-y-4">
        {openCount > 0 && (
          <Link href="/tasks/all" className="block">
            <SectionCard quiet>
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-tint-task/12 text-tint-task"><ListTodo size={19} /></div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-text-primary">{t("projects.allTasks")}</p>
                  <p className="mt-0.5 text-xs text-text-secondary">{t("projects.allTasksSub")} · {t("projects.openTasks", { n: openCount })}</p>
                </div>
                <ChevronRight size={18} className="text-text-secondary" />
              </div>
            </SectionCard>
          </Link>
        )}

        {summaries.length ? summaries.map(({ project: p, openTasks, doneTasks, progress }) => (
          <div key={p.id} className="group relative">
            <button onClick={() => router.push(`/projects/detail?id=${p.id}`)} className="w-full text-left">
              <div className="rounded-card border border-border bg-surface p-[18px] shadow-card transition-opacity active:opacity-70">
                <div className="flex items-center gap-2">
                  <span className="text-xl">{p.emoji}</span>
                  <span className="min-w-0 flex-1 truncate text-[17px] font-semibold text-text-primary">{p.name}</span>
                  <span className="shrink-0 text-xs font-medium text-text-secondary">{t("projects.openTasks", { n: openTasks })}</span>
                  <MoreHorizontal size={18} className="shrink-0 text-text-secondary"
                    onClick={(e) => { e.stopPropagation(); setEditingProject(p); }} />
                </div>
                <div className="mt-3">
                  <div className="h-1.5 overflow-hidden rounded-full bg-border">
                    <div className="h-full rounded-full bg-tint-project transition-all"
                      style={{ width: `${Math.round((progress ?? 0) * 100)}%` }} />
                  </div>
                  {progress !== null && (
                    <p className="mt-1.5 text-[11px] text-text-secondary">{t("project.progress", { done: doneTasks, total: doneTasks + openTasks, pct: Math.round(progress * 100) })}</p>
                  )}
                </div>
              </div>
            </button>
          </div>
        )) : (
          <SectionCard><EmptyState icon={<FolderOpen />}>{t("projects.empty")}<br /><span className="text-xs">{t("projects.emptySub")}</span></EmptyState></SectionCard>
        )}

        {archivedCount > 0 && (
          <Link href="/projects/archived" className="block">
            <SectionCard quiet>
              <div className="flex items-center gap-3">
                <Archive size={18} className="text-text-secondary" />
                <span className="flex-1 text-sm text-text-secondary">{t("projects.archived")}</span>
                <span className="text-xs text-text-secondary">{archivedCount}</span>
                <ChevronRight size={16} className="text-text-secondary" />
              </div>
            </SectionCard>
          </Link>
        )}

        {unassigned.length > 0 && (
          <SectionCard title={t("projects.unassigned")} quiet>
            <div className="divide-y divide-border/60">
              {unassigned.slice(0, 8).map((task) => (
                <TaskRow key={task.id} task={task} onEdit={() => setTaskSheet(true)} />
              ))}
            </div>
            {unassigned.length > 8 && <EmptyHint className="mt-2">+{unassigned.length - 8}</EmptyHint>}
          </SectionCard>
        )}
      </div>

      <button onClick={() => setShowNew(true)} aria-label={t("projects.new")}
        className="fixed bottom-24 right-[max(1rem,calc(50%-224px+1rem))] z-30 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-2xl text-background shadow-lg transition-transform active:scale-95">+</button>

      <NameEmojiDialog open={showNew} onClose={() => setShowNew(false)} title={t("projects.new")} nameLabel={t("projects.name")}
        onSave={(name, emoji) => createProject({ name, emoji })} />
      {editingProject && (
        <NameEmojiDialog open onClose={() => setEditingProject(null)} title={t("common.edit")} nameLabel={t("projects.name")}
          initialName={editingProject.name} initialEmoji={editingProject.emoji}
          extraAction={{ label: t("common.archive"), onClick: () => setArchived(editingProject.id, true) }}
          onSave={(name, emoji) => updateProject(editingProject.id, { name, emoji })} />
      )}
      <TaskEditSheet open={taskSheet} onClose={() => setTaskSheet(false)} />
    </main>
  );
}
