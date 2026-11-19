"use client";

import { Circle, CheckCircle2 } from "lucide-react";
import type { Task } from "@/types";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { cn } from "@/lib/cn";
import { REPEAT_LABEL, isOverdue } from "@/utils/business";
import { today as todayKey, dateKey } from "@/utils/date";
import { toPlainText } from "./markdown";

interface Props {
  task: Task;
  projectLabel?: string | null;
  onEdit?: () => void;
  large?: boolean;
}

export function TaskRow({ task, projectLabel, onEdit, large }: Props) {
  const { t, lang } = useT();
  const toggleTask = useAppStore((s) => s.toggleTask);
  const ts = todayKey();

  let dueLabel = "";
  let dueColor = "text-text-secondary";
  if (task.dueDate) {
    if (isOverdue(task, ts)) { dueLabel = t("task.overdue"); dueColor = "text-danger"; }
    else if (task.dueDate === ts) { dueLabel = t("common.today"); dueColor = "text-primary"; }
    else dueLabel = task.dueDate.slice(5).replace("-", "/");
  }

  return (
    <div className="flex items-start gap-2.5 py-2">
      <button onClick={() => toggleTask(task.id)} aria-label={task.done ? "重开" : "完成"} className="mt-0.5 shrink-0 p-0.5 transition-transform active:scale-90">
        {task.done
          ? <CheckCircle2 size={18} className="text-tint-habit" />
          : <Circle size={18} className="text-text-secondary" />}
      </button>
      <button onClick={onEdit} className="min-w-0 flex-1 text-left">
        <p className={cn(
          large ? "text-base" : "text-[15px]",
          "leading-snug",
          task.done ? "text-text-secondary line-through" : "text-text-primary",
        )}>{task.title}</p>
        {task.notes && !task.done && (
          <p className="mt-0.5 truncate text-xs text-text-secondary">{toPlainText(task.notes)}</p>
        )}
        {(projectLabel || task.repeatRule || dueLabel) && (
          <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-text-secondary">
            {projectLabel && <span>{projectLabel}</span>}
            {task.repeatRule && <span className="rounded bg-primary/10 px-1.5 py-0.5 text-primary">{REPEAT_LABEL[task.repeatRule][lang]}</span>}
            {dueLabel && <span className={dueColor}>{dueLabel}</span>}
          </div>
        )}
      </button>
    </div>
  );
}
