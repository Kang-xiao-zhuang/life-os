"use client";

import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, BookOpen, CalendarDays } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { useToday } from "@/lib/use-today";
import { addDays, today as todayKey, formatDate } from "@/utils/date";
import { dayCompletions, appendCompletedLines, journalIsEmpty } from "@/utils/business";
import { EMPTY_JOURNAL, type JournalEntry } from "@/types";
import { PageHeader } from "@/components/common/page-header";
import { SectionCard, EmptyHint } from "@/components/common/section-card";
import { Markdown, toPlainText } from "@/components/common/markdown";

const FIELDS = [
  { key: "done", labelKey: "journal.done", ph: "今天完成了什么…" },
  { key: "win", labelKey: "journal.win", ph: "最大的收获…" },
  { key: "problems", labelKey: "journal.problems", ph: "遇到的问题…" },
  { key: "tomorrowMit", labelKey: "journal.tomorrow", ph: "明天最重要的一件事…" },
] as const;

export default function JournalPage() {
  const { t, lang } = useT();
  const todayStr = useToday();
  const tasks = useAppStore((s) => s.tasks);
  const habits = useAppStore((s) => s.habits);
  const checks = useAppStore((s) => s.habitChecks);
  const journals = useAppStore((s) => s.journals);
  const saveJournal = useAppStore((s) => s.saveJournal);
  const hydrated = useAppStore((s) => s.hydrated);

  const [selected, setSelected] = useState("");
  const [draft, setDraft] = useState<JournalEntry>(EMPTY_JOURNAL(""));
  const [dirty, setDirty] = useState(false);
  const [readMode, setReadMode] = useState(false);
  const [showDate, setShowDate] = useState(false);

  // 初始跟随"今天"
  useEffect(() => { if (!selected && todayStr) { setSelected(todayStr); setDraft(journals.find((j) => j.date === todayStr) ?? EMPTY_JOURNAL(todayStr)); } }, [todayStr, journals, selected]);

  const saved = useMemo(() => journals.find((j) => j.date === selected) ?? null, [journals, selected]);
  const completions = useMemo(() => selected ? dayCompletions(tasks, habits, checks, selected) : { taskTitles: [], habitNames: [] }, [tasks, habits, checks, selected]);
  const canGoNext = selected < todayStr;

  const selectDate = (date: string) => {
    if (date > todayStr || date === selected) return;
    if (dirty) saveJournal(draft); // 换天前先存
    setSelected(date);
    setDraft(journals.find((j) => j.date === date) ?? EMPTY_JOURNAL(date));
    setDirty(false);
    setShowDate(false);
  };

  const changeField = (key: typeof FIELDS[number]["key"], value: string) => {
    setDraft((d) => ({ ...d, [key]: value }));
    setDirty(true);
  };

  const doSave = () => { saveJournal(draft); setDirty(false); };

  const fillCompleted = () => {
    const titles = [...completions.taskTitles, ...completions.habitNames];
    setDraft((d) => ({ ...d, done: appendCompletedLines(d.done, titles) }));
    setDirty(true);
  };

  if (!hydrated || !selected) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;

  const isPast = selected !== todayStr;
  const statusText = dirty ? t("common.unsaved") : saved && !journalIsEmpty(saved) ? t("common.saved") : t("home.journalEmpty");

  return (
    <main className="px-5 pt-6">
      <PageHeader title={t("journal.title")} />

      <SectionCard>
        {/* 日期选择行 */}
        <div className="mb-4 flex items-center justify-between gap-2">
          <div className="flex items-center gap-1">
            <button onClick={() => selectDate(addDays(selected, -1))} aria-label={t("common.yesterday")} className="flex h-8 w-8 items-center justify-center rounded-lg text-text-secondary hover:bg-border/50"><ChevronLeft size={20} /></button>
            <button onClick={() => setShowDate((v) => !v)} className="flex items-center gap-1.5 rounded-lg px-2 py-1 text-sm font-medium text-text-primary hover:bg-border/50">
              <CalendarDays size={16} />{isPast ? formatDate(selected, lang) : t("common.today")}
            </button>
            <button onClick={() => selectDate(addDays(selected, 1))} disabled={!canGoNext} aria-label={t("common.tomorrow")} className="flex h-8 w-8 items-center justify-center rounded-lg text-text-secondary hover:bg-border/50 disabled:opacity-25"><ChevronRight size={20} /></button>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-text-secondary">{statusText}</span>
            {saved && !journalIsEmpty(saved) && (
              <button onClick={() => setReadMode((v) => !v)} className="text-xs font-medium text-primary">{readMode ? t("journal.edit") : t("journal.read")}</button>
            )}
          </div>
        </div>

        {showDate && (
          <div className="mb-3 rounded-input border border-border bg-background p-2">
            <input type="date" value={selected} max={todayStr} onChange={(e) => e.target.value && selectDate(e.target.value)}
              className="w-full rounded-lg bg-background px-2 py-1.5 text-sm text-text-primary outline-none" />
          </div>
        )}
        {isPast && <div className="mb-3"><EmptyHint>{t("journal.past")}</EmptyHint></div>}

        {readMode && saved && !journalIsEmpty(saved) ? (
          <div className="space-y-4">
            {FIELDS.map((f) => (
              <div key={f.key}>
                <p className="mb-1 text-[11px] font-medium text-text-secondary">{t(f.labelKey)}</p>
                <Markdown text={saved[f.key]} />
              </div>
            ))}
          </div>
        ) : (
          <div className="space-y-4">
            {FIELDS.map((f) => (
              <div key={f.key}>
                <label className="mb-1.5 block text-[13px] font-medium text-text-primary">{t(f.labelKey)}</label>
                <textarea value={draft[f.key]} onChange={(e) => changeField(f.key, e.target.value)} rows={2} placeholder={f.ph}
                  className="w-full resize-none rounded-input border border-border bg-background px-3.5 py-2.5 text-sm leading-relaxed text-text-primary outline-none placeholder:text-text-secondary/60 focus:border-primary" />
              </div>
            ))}
            {(completions.taskTitles.length + completions.habitNames.length) > 0 && (
              <button onClick={fillCompleted} className="rounded-full border border-border px-3 py-1.5 text-xs font-medium text-primary">
                {t("journal.fillCompleted", { n: completions.taskTitles.length + completions.habitNames.length })}
              </button>
            )}
            <button onClick={doSave} disabled={!dirty}
              className="h-11 w-full rounded-btn bg-primary text-sm font-semibold text-background disabled:opacity-40">{t("common.save")}</button>
          </div>
        )}
      </SectionCard>

      {/* 历史 */}
      {journals.filter((j) => j.date < todayStr && !journalIsEmpty(j)).length > 0 && (
        <SectionCard title={t("journal.history")} quiet className="mt-5">
          <div className="space-y-1">
            {journals.filter((j) => j.date < todayStr && !journalIsEmpty(j))
              .sort((a, b) => b.date.localeCompare(a.date)).slice(0, 30).map((j) => (
                <button key={j.date} onClick={() => selectDate(j.date)} className="w-full rounded-xl px-2 py-2 text-left hover:bg-border/30">
                  <p className="text-sm font-medium text-primary">{formatDate(j.date, lang)}</p>
                  {(j.win || j.done) && <p className="mt-0.5 line-clamp-2 text-xs text-text-secondary">{toPlainText(j.win || j.done).slice(0, 80)}</p>}
                </button>
              ))}
          </div>
        </SectionCard>
      )}
    </main>
  );
}
