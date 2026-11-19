"use client";

import { useState } from "react";
import Link from "next/link";
import { Flame, Archive, ChevronRight } from "lucide-react";
import { useAppStore } from "@/store/app.store";
import { useT } from "@/lib/i18n";
import { useToday } from "@/lib/use-today";
import { habitsToday } from "@/utils/business";
import { SectionCard, EmptyState, EmptyHint } from "@/components/common/section-card";
import { HabitRow } from "@/components/common/habit-row";
import { HabitHeatmap } from "@/components/common/habit-heatmap";
import { NameEmojiDialog } from "@/components/common/name-emoji-dialog";
import type { Habit } from "@/types";

export default function HabitsPage() {
  const { t } = useT();
  const todayStr = useToday();
  const habits = useAppStore((s) => s.habits);
  const checks = useAppStore((s) => s.habitChecks);
  const createHabit = useAppStore((s) => s.createHabit);
  const updateHabit = useAppStore((s) => s.updateHabit);
  const setArchived = useAppStore((s) => s.setHabitArchived);
  const hydrated = useAppStore((s) => s.hydrated);

  const [showNew, setShowNew] = useState(false);
  const [editing, setEditing] = useState<Habit | null>(null);
  const items = habitsToday(habits, checks, todayStr);
  const archivedCount = habits.filter((h) => h.archived).length;
  const doneCount = items.filter((i) => i.checkedToday).length;

  if (!hydrated) return <div className="px-5 pt-6"><div className="h-40 animate-pulse rounded-card bg-surface" /></div>;

  return (
    <main className="px-5 pt-6">
      <h1 className="text-2xl font-bold text-text-primary">{t("habits.title")}</h1>

      <div className="mt-5 space-y-4">
        {items.length ? (
          <SectionCard title={t("habits.today")} trailing={t("home.habitsDone", { done: doneCount, total: items.length })}>
            <div className="space-y-1">
              {items.map((item) => <HabitRow key={item.habit.id} item={item} onLongPress={() => setEditing(item.habit)} />)}
            </div>
            <div className="mt-2"><EmptyHint>{t("habits.hint")}</EmptyHint></div>
          </SectionCard>
        ) : (
          <SectionCard><EmptyState icon={<Flame />}>{t("habits.empty")}<br /><span className="text-xs">{t("habits.emptySub")}</span></EmptyState></SectionCard>
        )}

        <SectionCard title={t("habits.month")} quiet><HabitHeatmap /></SectionCard>

        {archivedCount > 0 && (
          <Link href="/habits/archived" className="block">
            <SectionCard quiet>
              <div className="flex items-center gap-3">
                <Archive size={18} className="text-text-secondary" />
                <span className="flex-1 text-sm text-text-secondary">{t("habits.archived")}</span>
                <span className="text-xs text-text-secondary">{archivedCount}</span>
                <ChevronRight size={16} className="text-text-secondary" />
              </div>
            </SectionCard>
          </Link>
        )}
      </div>

      <button onClick={() => setShowNew(true)} aria-label={t("habits.new")}
        className="fixed bottom-24 right-[max(1rem,calc(50%-224px+1rem))] z-30 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-2xl text-background shadow-lg transition-transform active:scale-95">+</button>

      <NameEmojiDialog open={showNew} onClose={() => setShowNew(false)} title={t("habits.new")} nameLabel={t("habits.name")} kind="habit"
        onSave={(name, emoji) => createHabit({ name, emoji })} />
      {editing && (
        <NameEmojiDialog open onClose={() => setEditing(null)} title={t("common.edit")} nameLabel={t("habits.name")} kind="habit"
          initialName={editing.name} initialEmoji={editing.emoji}
          extraAction={{ label: t("common.archive"), onClick: () => setArchived(editing.id, true) }}
          onSave={(name, emoji) => updateHabit(editing.id, { name, emoji })} />
      )}
    </main>
  );
}
