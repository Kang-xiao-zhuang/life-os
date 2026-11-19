"use client";

import { useState } from "react";
import { RotateCcw, Trash2 } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { PageHeader } from "@/components/common/page-header";
import { ConfirmDialog } from "@/components/common/modal";

export default function ArchivedHabitsPage() {
  const { t } = useT();
  const habits = useAppStore((s) => s.habits);
  const checks = useAppStore((s) => s.habitChecks);
  const setArchived = useAppStore((s) => s.setHabitArchived);
  const deleteHabit = useAppStore((s) => s.deleteHabit);
  const [toDelete, setToDelete] = useState<string | null>(null);
  const archived = habits.filter((h) => h.archived);
  const checkCount = (id: string) => checks.filter((c) => c.habitId === id).length;

  return (
    <>
      <PageHeader title={t("habits.archived")} back />
      <main className="space-y-3 px-5 pt-5">
        {archived.length === 0 && <p className="py-10 text-center text-sm text-text-secondary">{t("common.empty")}</p>}
        {archived.map((h) => (
          <div key={h.id} className="flex items-center gap-3 rounded-card border border-border bg-surface p-4">
            <span className="text-xl">{h.emoji}</span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm text-text-primary">{h.name}</p>
              <p className="text-[11px] text-text-secondary">{checkCount(h.id)} 次打卡</p>
            </div>
            <button onClick={() => setArchived(h.id, false)} aria-label={t("common.restore")} className="flex h-9 w-9 items-center justify-center rounded-lg text-tint-habit hover:bg-tint-habit/10"><RotateCcw size={18} /></button>
            <button onClick={() => setToDelete(h.id)} aria-label={t("common.delete")} className="flex h-9 w-9 items-center justify-center rounded-lg text-danger hover:bg-danger/10"><Trash2 size={18} /></button>
          </div>
        ))}
      </main>
      <ConfirmDialog open={!!toDelete} onClose={() => setToDelete(null)} danger
        title={t("common.delete")}
        message={toDelete ? t("habits.deleteWarn", { n: checkCount(toDelete) }) : ""}
        onConfirm={() => { if (toDelete) deleteHabit(toDelete); }} />
    </>
  );
}
