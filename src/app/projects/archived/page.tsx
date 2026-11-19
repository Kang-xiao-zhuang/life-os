"use client";

import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { PageHeader } from "@/components/common/page-header";
import { SectionCard } from "@/components/common/section-card";
import { ConfirmDialog } from "@/components/common/modal";
import { Archive, RotateCcw, Trash2 } from "lucide-react";
import { useState } from "react";

export default function ArchivedProjectsPage() {
  const { t } = useT();
  const projects = useAppStore((s) => s.projects);
  const setArchived = useAppStore((s) => s.setProjectArchived);
  const deletePermanently = useAppStore((s) => s.deleteProjectPermanently);
  const [toDelete, setToDelete] = useState<string | null>(null);
  const archived = projects.filter((p) => p.archived);

  return (
    <>
      <PageHeader title={t("projects.archived")} back />
      <main className="space-y-3 px-5 pt-5">
        {archived.length === 0 && <p className="py-10 text-center text-sm text-text-secondary">{t("common.empty")}</p>}
        {archived.map((p) => (
          <div key={p.id} className="flex items-center gap-3 rounded-card border border-border bg-surface p-4">
            <span className="text-xl">{p.emoji}</span>
            <span className="min-w-0 flex-1 truncate text-sm text-text-primary">{p.name}</span>
            <button onClick={() => setArchived(p.id, false)} aria-label={t("common.restore")} className="flex h-9 w-9 items-center justify-center rounded-lg text-tint-habit hover:bg-tint-habit/10"><RotateCcw size={18} /></button>
            <button onClick={() => setToDelete(p.id)} aria-label={t("common.delete")} className="flex h-9 w-9 items-center justify-center rounded-lg text-danger hover:bg-danger/10"><Trash2 size={18} /></button>
          </div>
        ))}
        <p className="pt-2 text-xs leading-relaxed text-text-secondary">{t("settings.localOnly")}</p>
      </main>
      <ConfirmDialog open={!!toDelete} onClose={() => setToDelete(null)} danger
        title={t("common.delete")} message={t("projects.emptySub")}
        onConfirm={() => { if (toDelete) deletePermanently(toDelete); }} />
    </>
  );
}
