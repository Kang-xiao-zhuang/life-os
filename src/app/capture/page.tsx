"use client";

import { useState } from "react";
import { Inbox, Plus, X, ListTodo } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { SectionCard, EmptyState, EmptyHint } from "@/components/common/section-card";
import { ConfirmDialog } from "@/components/common/modal";
import type { Capture } from "@/types";

export default function CapturePage() {
  const { t } = useT();
  const captures = useAppStore((s) => s.captures);
  const capture = useAppStore((s) => s.capture);
  const deleteCapture = useAppStore((s) => s.deleteCapture);
  const convertCapture = useAppStore((s) => s.convertCapture);
  const hydrated = useAppStore((s) => s.hydrated);
  const [text, setText] = useState("");
  const [converting, setConverting] = useState<{ id: string; text: string } | null>(null);

  const inbox = captures.filter((c) => !c.processed);

  const submit = () => {
    if (!text.trim()) return;
    capture(text);
    setText("");
  };

  const doConvert = () => {
    if (converting) convertCapture(converting.id);
    setConverting(null);
  };

  if (!hydrated) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;

  return (
    <main className="px-5 pt-6">
      <h1 className="text-2xl font-bold text-text-primary">{t("capture.title")}</h1>

      <SectionCard className="mt-5">
        <textarea value={text} onChange={(e) => setText(e.target.value)} rows={3}
          placeholder={t("capture.placeholder")}
          className="w-full resize-none rounded-input border border-border bg-background px-3.5 py-3 text-sm text-text-primary outline-none placeholder:text-text-secondary/70 focus:border-primary" />
        <div className="mt-2.5 flex items-center gap-2.5">
          <button onClick={submit} disabled={!text.trim()}
            className="flex h-9 items-center gap-1.5 rounded-btn bg-primary px-4 text-sm font-semibold text-background disabled:opacity-40">
            <Plus size={16} />{t("capture.submit")}
          </button>
          {text && <button onClick={() => setText("")} aria-label={t("common.cancel")} className="flex h-9 w-9 items-center justify-center rounded-lg text-text-secondary hover:bg-border/50"><X size={18} /></button>}
        </div>
      </SectionCard>

      <SectionCard title={t("capture.inbox")} quiet className="mt-5" trailing={inbox.length ? String(inbox.length) : undefined}>
        {inbox.length ? (
          <div className="divide-y divide-border/60">
            {inbox.map((c) => (
              <div key={c.id} className="flex items-start gap-2 py-2.5">
                <p className="min-w-0 flex-1 whitespace-pre-wrap break-words text-[15px] leading-snug text-text-primary">{c.text}</p>
                <button onClick={() => setConverting(c)} aria-label={t("capture.toTask")} className="shrink-0 p-1 text-tint-habit hover:opacity-70"><ListTodo size={19} /></button>
                <button onClick={() => deleteCapture(c.id)} aria-label={t("common.delete")} className="shrink-0 p-1 text-text-secondary hover:text-danger"><X size={17} /></button>
              </div>
            ))}
            <div className="pt-2"><EmptyHint>{t("capture.help")}</EmptyHint></div>
          </div>
        ) : <EmptyState icon={<Inbox />}>{t("capture.empty")}<br /><span className="text-xs">{t("capture.emptySub")}</span></EmptyState>}
      </SectionCard>

      <ConfirmDialog open={!!converting} onClose={() => setConverting(null)} onConfirm={doConvert}
        title={t("capture.toTask")} message={converting?.text ?? ""} />
    </main>
  );
}
