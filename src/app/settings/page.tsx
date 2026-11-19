"use client";

import { useRef, useState } from "react";
import { Palette, Globe, Database, Download, Upload, Info, ChevronRight, Check } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { useTheme } from "@/lib/use-theme";
import { buildBackup, downloadBackup, parseBackup, previewBackup, type BackupPreview } from "@/lib/backup";
import type { BackupData } from "@/types";
import { PageHeader } from "@/components/common/page-header";
import { SectionCard } from "@/components/common/section-card";
import { Modal } from "@/components/common/modal";
import { SCHEMA_VERSION, type ThemeMode, type AppLanguage } from "@/types";

const VERSION = process.env.NEXT_PUBLIC_APP_VERSION ?? "1.0.0";
const DAY = 86400000;

export default function SettingsPage() {
  const { t, lang } = useT();
  useTheme();
  const settings = useAppStore((s) => s.settings);
  const setThemeMode = useAppStore((s) => s.setThemeMode);
  const setLanguage = useAppStore((s) => s.setLanguage);
  const markBackedUp = useAppStore((s) => s.markBackedUp);
  const replaceAll = useAppStore((s) => s.replaceAll);
  const hydrated = useAppStore((s) => s.hydrated);
  const fileRef = useRef<HTMLInputElement>(null);

  const [pending, setPending] = useState<{ data: BackupData; preview: BackupPreview } | null>(null);
  const [msg, setMsg] = useState("");

  const onExport = () => {
    const s = useAppStore.getState();
    const data = buildBackup({
      projects: s.projects, tasks: s.tasks, captures: s.captures, habits: s.habits,
      habitChecks: s.habitChecks, journals: s.journals, settings: s.settings,
    }, VERSION);
    downloadBackup(data);
    markBackedUp();
  };

  const onImportFile = async (file: File) => {
    setMsg("");
    try {
      const data = parseBackup(await file.text());
      setPending({ data, preview: previewBackup(data) });
    } catch (e) {
      const key = e instanceof Error ? e.message : "invalid";
      setMsg(key.startsWith("schema:") ? t("backup.schemaMismatch", { a: key.slice(7), b: SCHEMA_VERSION }) : t("backup.invalid"));
    }
  };

  const doImport = () => {
    if (!pending) return;
    replaceAll({
      projects: pending.data.projects, tasks: pending.data.tasks, captures: pending.data.captures,
      habits: pending.data.habits, habitChecks: pending.data.habitChecks, journals: pending.data.journals,
      settings: pending.data.settings,
    });
    markBackedUp();
    setPending(null);
    setMsg(t("backup.success"));
  };

  const backupDays = settings.lastBackupAt ? Math.floor((Date.now() - settings.lastBackupAt) / DAY) : null;
  const backupStale = backupDays === null || backupDays >= 14;

  if (!hydrated) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;

  return (
    <>
      <PageHeader title={t("settings.title")} />
      <main className="space-y-4 px-5 pt-5 pb-10">
        <SectionCard title={t("settings.appearance")}>
          <div className="mb-1.5 text-xs font-medium text-text-secondary">{t("settings.theme")}</div>
          <Segmented<ThemeMode>
            value={settings.themeMode}
            options={[
              { value: "system", label: t("settings.theme.system") },
              { value: "light", label: t("settings.theme.light") },
              { value: "dark", label: t("settings.theme.dark") },
            ]}
            onChange={setThemeMode}
          />
          <div className="mb-1.5 mt-4 text-xs font-medium text-text-secondary">{t("settings.language")}</div>
          <Segmented<AppLanguage>
            value={settings.language}
            options={[
              { value: "system", label: t("settings.language.system") },
              { value: "zh", label: "中文" },
              { value: "en", label: "English" },
            ]}
            onChange={setLanguage}
          />
        </SectionCard>

        <SectionCard title={t("settings.data")}>
          <p className={`text-xs ${backupStale ? "text-warning" : "text-text-secondary"}`}>
            {t("settings.lastBackup", { when: backupDays === null ? t("settings.never") : backupDays === 0 ? t("common.today") : t("settings.daysAgo", { n: backupDays }) })}
          </p>
          <p className="mt-1 text-[11px] text-text-secondary">{t("settings.backupHint")}</p>
          <div className="mt-3 flex gap-2.5">
            <button onClick={onExport} className="flex h-10 flex-1 items-center justify-center gap-1.5 rounded-btn border border-border text-sm font-medium text-text-primary active:opacity-70"><Download size={16} />{t("settings.export")}</button>
            <button onClick={() => fileRef.current?.click()} className="flex h-10 flex-1 items-center justify-center gap-1.5 rounded-btn border border-border text-sm font-medium text-text-primary active:opacity-70"><Upload size={16} />{t("settings.import")}</button>
          </div>
          {msg && <p className="mt-2 text-xs text-primary">{msg}</p>}
          <input ref={fileRef} type="file" accept="application/json,.json" hidden onChange={(e) => { const f = e.target.files?.[0]; if (f) onImportFile(f); e.target.value = ""; }} />
        </SectionCard>

        <SectionCard title={t("settings.about")}>
          <div className="flex items-center justify-between text-sm">
            <span className="flex items-center gap-2 text-text-primary"><Info size={16} />{t("app.name")}</span>
            <span className="text-text-secondary">{t("settings.version", { v: VERSION })}</span>
          </div>
          <p className="mt-3 text-xs leading-relaxed text-text-secondary">{t("settings.localOnly")}</p>
        </SectionCard>
      </main>

      {pending && (
        <Modal open onClose={() => setPending(null)} title={t("backup.importTitle")} footer={
          <button onClick={doImport} className="h-11 w-full rounded-btn bg-primary text-sm font-semibold text-background">{t("backup.replace")}</button>
        }>
          <div className="space-y-3 text-sm">
            <p className="text-text-secondary">{t("backup.exportedAt")}：{new Date(pending.preview.exportedAt).toLocaleString()}</p>
            <p className="text-text-primary">{t("backup.counts", { p: pending.preview.projects, t: pending.preview.tasks, h: pending.preview.habits, j: pending.preview.journals })}</p>
            <p className="rounded-lg bg-warning/10 p-3 text-xs leading-relaxed text-text-secondary">{t("backup.replaceHint")}</p>
          </div>
        </Modal>
      )}
    </>
  );
}

function Segmented<T extends string>({ value, options, onChange }: { value: T; options: { value: T; label: string }[]; onChange: (v: T) => void }) {
  return (
    <div className="flex rounded-xl bg-surface-quiet p-1">
      {options.map((o) => (
        <button key={o.value} onClick={() => onChange(o.value)} className={`flex h-9 flex-1 items-center justify-center rounded-lg text-xs font-medium transition-colors ${value === o.value ? "bg-surface text-primary shadow-sm" : "text-text-secondary"}`}>
          {o.label}
        </button>
      ))}
    </div>
  );
}
