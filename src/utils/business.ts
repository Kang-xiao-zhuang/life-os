// 纯业务逻辑：从 store 的原始数据实时推导，不写库。对齐 Kotlin 的 Service/Repository 规则。
import type { Task, RepeatRule, HabitCheck, Habit, JournalEntry, Project, Capture } from "@/types";
import { isJournalEmpty } from "@/types";
import { addDays, addMonths, addYears, dateKey, weekDays } from "./date";

// ── 重复任务 ──────────────────────────────────────────────

export const REPEAT_LABEL: Record<RepeatRule, { zh: string; en: string }> = {
  DAILY: { zh: "每天", en: "Daily" },
  WEEKLY: { zh: "每周", en: "Weekly" },
  MONTHLY: { zh: "每月", en: "Monthly" },
  YEARLY: { zh: "每年", en: "Yearly" },
};

/** 按 dueDate 锚定算下一次（优先 dueDate，无日期才用 today）。 */
export function nextOccurrence(task: Task, todayStr: string): string | null {
  if (!task.repeatRule) return null;
  const base = task.dueDate ?? todayStr;
  return advanceRepeat(base, task.repeatRule);
}

export function advanceRepeat(from: string, rule: RepeatRule): string {
  switch (rule) {
    case "DAILY": return addDays(from, 1);
    case "WEEKLY": return addDays(from, 7);
    case "MONTHLY": return addMonths(from, 1);
    case "YEARLY": return addYears(from, 1);
  }
}

/** 生成下一条重复任务：强制 isMit=false、done=false，其余复制。 */
export function makeNextTask(task: Task, on: string, now: string, id: string): Task {
  return {
    ...task,
    id,
    dueDate: on,
    isMit: false,
    done: false,
    completedAt: null,
    createdAt: now,
    updatedAt: now,
  };
}

/**
 * 找到「刚生成、未编辑、未勾选」的那一条，用于取消勾选时收回。
 * 匹配条件与 Kotlin 一致：同 title/repeatRule/dueDate 且未完成，取最新一条。
 */
export function findGeneratedOccurrence(tasks: Task[], source: Task, on: string): Task | null {
  const candidates = tasks.filter(
    (t) => !t.done && t.title === source.title && t.repeatRule === source.repeatRule && t.dueDate === on,
  );
  if (!candidates.length) return null;
  return candidates.reduce((a, b) => (a.createdAt >= b.createdAt ? a : b));
}

// ── 任务状态 ──────────────────────────────────────────────

export const isOverdue = (t: Task, todayStr: string): boolean =>
  !t.done && t.dueDate !== null && t.dueDate < todayStr;

export const isDueToday = (t: Task, todayStr: string): boolean =>
  !t.done && t.dueDate === todayStr;

/** 所有待办排序：逾期在前，再按 dueDate，无日期最后。 */
export function sortOpenTasks(tasks: Task[], todayStr: string): Task[] {
  const rank = (t: Task) => {
    if (isOverdue(t, todayStr)) return 0;
    if (isDueToday(t, todayStr)) return 1;
    if (t.dueDate && t.dueDate > todayStr) return 2;
    return 3;
  };
  return [...tasks].sort((a, b) => {
    const r = rank(a) - rank(b);
    if (r) return r;
    if (a.dueDate && b.dueDate) return a.dueDate.localeCompare(b.dueDate);
    if (a.dueDate) return -1;
    if (b.dueDate) return 1;
    return b.createdAt.localeCompare(a.createdAt);
  });
}

// ── 习惯 ─────────────────────────────────────────────────

/** 某习惯在 dateKey 是否打卡。 */
export const isChecked = (checks: HabitCheck[], habitId: string, date: string): boolean =>
  checks.some((c) => c.habitId === habitId && c.date === date);

/**
 * streak：今日未打卡不算断，从昨天起量（对齐 Kotlin habitRepository.streakOf）。
 */
export function habitStreak(checks: HabitCheck[], habitId: string, todayStr: string): number {
  const dates = new Set(checks.filter((c) => c.habitId === habitId).map((c) => c.date));
  let cursor = dates.has(todayStr) ? todayStr : addDays(todayStr, -1);
  let count = 0;
  while (dates.has(cursor) && count < 3650) {
    count++;
    cursor = addDays(cursor, -1);
  }
  return count;
}

/** 本周七个点（周一→周日），今天之后的算未来=false。 */
export function habitWeek(checks: HabitCheck[], habitId: string, todayStr: string): boolean[] {
  const dates = new Set(checks.filter((c) => c.habitId === habitId).map((c) => c.date));
  return weekDays(todayStr).map((d) => dates.has(d));
}

/** 今日习惯的聚合视图。 */
export interface HabitToday {
  habit: Habit;
  checkedToday: boolean;
  streak: number;
  week: boolean[];
}

export function habitsToday(habits: Habit[], checks: HabitCheck[], todayStr: string): HabitToday[] {
  return habits
    .filter((h) => !h.archived)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map((h) => ({
      habit: h,
      checkedToday: isChecked(checks, h.id, todayStr),
      streak: habitStreak(checks, h.id, todayStr),
      week: habitWeek(checks, h.id, todayStr),
    }));
}

/** 月度热力图：每天的完成度 0..1（当天打卡习惯数 ÷ 当前活跃习惯数）。 */
export interface HabitMonthCell {
  date: string;
  completion: number | null; // null = 未来
}

export function habitMonth(checks: HabitCheck[], habitCount: number, days: string[], todayStr: string): HabitMonthCell[] {
  const byDay = new Map<string, number>();
  for (const c of checks) byDay.set(c.date, (byDay.get(c.date) ?? 0) + 1);
  return days.map((date) => {
    if (date > todayStr) return { date, completion: null };
    if (habitCount <= 0) return { date, completion: 0 };
    return { date, completion: Math.min(1, (byDay.get(date) ?? 0) / habitCount) };
  });
}

// ── 项目 ─────────────────────────────────────────────────

export interface ProjectSummary {
  project: Project;
  openTasks: number;
  doneTasks: number;
  progress: number | null; // 0..1，无任务时 null
}

export function projectSummaries(projects: Project[], tasks: Task[]): ProjectSummary[] {
  return projects
    .filter((p) => !p.archived)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map((p) => {
      const own = tasks.filter((t) => t.projectId === p.id);
      const doneTasks = own.filter((t) => t.done).length;
      const openTasks = own.length - doneTasks;
      return { project: p, openTasks, doneTasks, progress: own.length ? doneTasks / own.length : null };
    });
}

// ── 复盘 ─────────────────────────────────────────────────

/** 把已完成的任务标题追加进复盘的"今天完成了什么"，去重（去掉 Markdown "- " 前缀后精确比对）。 */
export function appendCompletedLines(field: string, titles: string[]): string {
  const existing = field.split("\n").map((l) => l.replace(/^\s*-\s*/, "").trim()).filter(Boolean);
  const lines = field.trim() ? field.trimEnd().split("\n") : [];
  for (const title of titles) {
    const clean = title.trim();
    if (clean && !existing.includes(clean)) {
      lines.push(`- ${clean}`);
      existing.push(clean);
    }
  }
  return lines.join("\n");
}

/** 某天已完成的标题 + 已打卡的习惯名（供复盘"带出已完成"）。 */
export function dayCompletions(
  tasks: Task[], habits: Habit[], checks: HabitCheck[], date: string,
): { taskTitles: string[]; habitNames: string[] } {
  return {
    taskTitles: tasks.filter((t) => t.done && t.completedAt && dateKey(new Date(t.completedAt)) === date).map((t) => t.title),
    habitNames: habits.filter((h) => isChecked(checks, h.id, date)).map((h) => h.name),
  };
}

export const journalIsEmpty = isJournalEmpty;

// ── 活跃度 ────────────────────────────────────────────────

export interface ActivityPoint {
  date: string; // "MM-DD"
  count: number;
}

/** 近 N 天活跃：完成任务数 + 习惯打卡数，按本地日期归集。 */
export function activityDays(
  tasks: Task[], checks: HabitCheck[], todayStr: string, days = 30,
): ActivityPoint[] {
  const dates = Array.from({ length: days }, (_, i) => addDays(todayStr, i - (days - 1)));
  const byDay = new Map<string, number>();
  for (const t of tasks) {
    if (!t.done || !t.completedAt) continue;
    const key = dateKey(new Date(t.completedAt));
    byDay.set(key, (byDay.get(key) ?? 0) + 1);
  }
  for (const c of checks) byDay.set(c.date, (byDay.get(c.date) ?? 0) + 1);
  return dates.map((date) => ({ date: date.slice(5), count: byDay.get(date) ?? 0 }));
}

// ── Dashboard 聚合 ────────────────────────────────────────

export interface DashboardData {
  mit: Task[];
  dueToday: Task[];
  overdue: Task[];
  habits: HabitToday[];
  journal: JournalEntry | null;
  carriedMit: string;
  inboxCount: number;
  todayDone: number;
  todayHabits: number;
  hasJournal: boolean;
}

export function dashboardData(
  tasks: Task[], habits: Habit[], checks: HabitCheck[], journals: JournalEntry[],
  captures: Capture[], todayStr: string,
): DashboardData {
  const mit = sortOpenTasks(tasks.filter((t) => t.isMit && !t.done), todayStr).slice(0, 2);
  const mitIds = new Set(mit.map((t) => t.id));
  const due = sortOpenTasks(tasks.filter((t) => isDueToday(t, todayStr) && !mitIds.has(t.id)), todayStr);
  const overdue = sortOpenTasks(tasks.filter((t) => isOverdue(t, todayStr)), todayStr);
  const yesterday = addDays(todayStr, -1);
  const yJournal = journals.find((j) => j.date === yesterday);
  const todayJournal = journals.find((j) => j.date === todayStr) ?? null;
  const todayHabits = habitsToday(habits, checks, todayStr);
  return {
    mit,
    dueToday: due,
    overdue,
    habits: todayHabits,
    journal: todayJournal,
    carriedMit: yJournal?.tomorrowMit.trim() ?? "",
    inboxCount: captures.filter((c) => !c.processed).length,
    todayDone: tasks.filter((t) => t.done && t.completedAt && dateKey(new Date(t.completedAt)) === todayStr).length,
    todayHabits: todayHabits.filter((h) => h.checkedToday).length,
    hasJournal: todayJournal !== null,
  };
}
