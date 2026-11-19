"use client";

import { useState } from "react";
import { Check, Trash2 } from "lucide-react";
import type { Task, RepeatRule } from "@/types";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { cn } from "@/lib/cn";
import { dateKey, addDays, today as todayKey } from "@/utils/date";
import { REPEAT_LABEL } from "@/utils/business";
import { Modal } from "./modal";

interface Props {
  open: boolean;
  onClose: () => void;
  editing?: Task | null;
  defaultProjectId?: string | null;
  defaultMit?: boolean;
  initialTitle?: string;
}

const REPEATS: (RepeatRule | null)[] = [null, "DAILY", "WEEKLY", "MONTHLY", "YEARLY"];

export function TaskEditSheet({ open, onClose, editing, defaultProjectId, defaultMit, initialTitle }: Props) {
  const { t, lang } = useT();
  const projects = useAppStore((s) => s.projects);
  const mitCount = useAppStore((s) => s.tasks.filter((x) => x.isMit && !x.done).length);
  const createTask = useAppStore((s) => s.createTask);
  const updateTask = useAppStore((s) => s.updateTask);
  const deleteTask = useAppStore((s) => s.deleteTask);

  const [title, setTitle] = useState(editing?.title ?? initialTitle ?? "");
  const [notes, setNotes] = useState(editing?.notes ?? "");
  const [dueDate, setDueDate] = useState<string | null>(editing?.dueDate ?? null);
  const [repeatRule, setRepeatRule] = useState<RepeatRule | null>(editing?.repeatRule ?? null);
  const [isMit, setIsMit] = useState(editing?.isMit ?? defaultMit ?? false);
  const [projectId, setProjectId] = useState<string | null>(editing?.projectId ?? defaultProjectId ?? null);

  // 打开时若是新建，重置为空（editing 变化时需要重置，这里用 key 重建组件处理）
  const activeProjects = projects.filter((p) => !p.archived);
  const othersMit = mitCount - (editing?.isMit ? 1 : 0);
  const showMitWarning = isMit && othersMit >= 2;

  const save = () => {
    if (!title.trim()) return;
    const input = { title, notes, dueDate, repeatRule, isMit, projectId };
    if (editing) updateTask(editing.id, input);
    else createTask(input);
    onClose();
  };

  const inputCls = "w-full rounded-input border border-border bg-background px-3.5 py-2.5 text-sm text-text-primary outline-none focus:border-primary";
  const labelCls = "mb-2 text-xs font-medium text-text-secondary";
  const chip = (on: boolean) => cn(
    "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors",
    on ? "border-primary bg-primary/10 text-primary" : "border-border text-text-secondary",
  );

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editing ? t("task.edit") : t("task.new")}
      footer={
        <div className="flex gap-3">
          {editing && (
            <button onClick={() => { deleteTask(editing.id); onClose(); }} aria-label={t("common.delete")}
              className="flex h-11 w-11 shrink-0 items-center justify-center rounded-btn border border-border text-danger">
              <Trash2 size={18} />
            </button>
          )}
          <button onClick={onClose} className="h-11 flex-1 rounded-btn border border-border text-sm font-medium text-text-secondary">{t("common.cancel")}</button>
          <button onClick={save} disabled={!title.trim()} className="h-11 flex-[1.5] rounded-btn bg-primary text-sm font-semibold text-background disabled:opacity-40">{t("common.save")}</button>
        </div>
      }
    >
      <div className="space-y-5">
        <div>
          <label className={labelCls}>{t("task.title")}</label>
          <input autoFocus value={title} onChange={(e) => setTitle(e.target.value)} className={inputCls} placeholder={t("task.title")} />
        </div>
        <div>
          <label className={labelCls}>{t("task.notes")}</label>
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} className={cn(inputCls, "resize-none")} />
        </div>

        <div>
          <label className={labelCls}>{t("task.dueDate")}</label>
          <div className="flex flex-wrap gap-2">
            <button className={chip(!dueDate)} onClick={() => setDueDate(null)}>{t("task.noDue")}</button>
            <button className={chip(dueDate === todayKey())} onClick={() => setDueDate(todayKey())}>{t("common.today")}</button>
            <button className={chip(dueDate === addDays(todayKey(), 1))} onClick={() => setDueDate(addDays(todayKey(), 1))}>{t("common.tomorrow")}</button>
            <label className={cn(chip(!!dueDate && dueDate !== todayKey() && dueDate !== addDays(todayKey(), 1)), "cursor-pointer")}>
              {dueDate && dueDate !== todayKey() && dueDate !== addDays(todayKey(), 1) ? dueDate : t("task.setDate")}
              <input type="date" value={dueDate ?? ""} onChange={(e) => setDueDate(e.target.value || null)} className="sr-only" />
            </label>
          </div>
        </div>

        <div>
          <label className={labelCls}>{t("task.repeat")}</label>
          <div className="flex flex-wrap gap-2">
            {REPEATS.map((r) => (
              <button key={r ?? "none"} className={chip(repeatRule === r)} onClick={() => setRepeatRule(r)}>
                {r ? REPEAT_LABEL[r][lang] : t("task.noRepeat")}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className={labelCls}>{t("task.project")}</label>
          <div className="flex flex-wrap gap-2">
            <button className={chip(!projectId)} onClick={() => setProjectId(null)}>{t("task.noProject")}</button>
            {activeProjects.map((p) => (
              <button key={p.id} className={chip(projectId === p.id)} onClick={() => setProjectId(p.id)}>
                {p.emoji && `${p.emoji} `}{p.name}
              </button>
            ))}
          </div>
        </div>

        <div>
          <button className={chip(isMit)} onClick={() => setIsMit((v) => !v)}>
            <span className="flex items-center gap-1.5">{isMit && <Check size={14} />}⭐ {t("task.mit")}</span>
          </button>
          {showMitWarning && <p className="mt-2 text-xs text-danger">{t("home.mitWarning")}</p>}
        </div>
      </div>
    </Modal>
  );
}
