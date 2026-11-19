import { create } from "zustand";
import type {
  Project, Task, Capture, Habit, HabitCheck, JournalEntry, Settings,
  RepeatRule, RescheduledTask, ThemeMode, AppLanguage,
} from "@/types";
import { EMPTY_JOURNAL, isJournalEmpty } from "@/types";
import { STORAGE_KEYS, readJSON, writeJSON, writeJSONDeferred, flushWrites } from "@/lib/storage";
import { createId } from "@/lib/id";
import { dateKey, today as todayKey, addDays } from "@/utils/date";
import { makeNextTask, findGeneratedOccurrence, nextOccurrence } from "@/utils/business";

const DEFAULT_SETTINGS: Settings = {
  themeMode: "system",
  language: "system",
  lastBackupAt: 0,
  accentHue: null,
  accentSat: null,
  accentLight: null,
};

export interface TaskInput {
  title: string;
  notes?: string;
  projectId?: string | null;
  dueDate?: string | null;
  isMit?: boolean;
  repeatRule?: RepeatRule | null;
}
export interface ProjectInput { name: string; emoji?: string }
export interface HabitInput { name: string; emoji?: string }

interface AppState {
  projects: Project[];
  tasks: Task[];
  captures: Capture[];
  habits: Habit[];
  habitChecks: HabitCheck[];
  journals: JournalEntry[];
  settings: Settings;
  hydrated: boolean;

  hydrate: () => void;
  resetAll: () => void;
  replaceAll: (data: {
    projects: Project[]; tasks: Task[]; captures: Capture[]; habits: Habit[];
    habitChecks: HabitCheck[]; journals: JournalEntry[]; settings?: Partial<Settings>;
  }) => void;

  // settings
  setThemeMode: (m: ThemeMode) => void;
  setLanguage: (l: AppLanguage) => void;
  setAccentColor: (hue: number | null, sat?: number | null, light?: number | null) => void;
  markBackedUp: () => void;

  // projects
  createProject: (input: ProjectInput) => Project;
  updateProject: (id: string, input: Partial<ProjectInput>) => void;
  setProjectArchived: (id: string, archived: boolean) => void;
  deleteProjectPermanently: (id: string) => void;

  // tasks
  createTask: (input: TaskInput) => Task;
  updateTask: (id: string, input: Partial<TaskInput>) => void;
  toggleTask: (id: string, todayStr?: string) => void;
  deleteTask: (id: string) => void;
  rescheduleOverdue: (todayStr?: string) => RescheduledTask[];
  undoReschedule: (items: RescheduledTask[]) => void;

  // captures
  capture: (text: string) => Capture;
  convertCapture: (id: string, projectId?: string | null) => Task | null;
  deleteCapture: (id: string) => void;

  // habits
  createHabit: (input: HabitInput) => Habit;
  updateHabit: (id: string, input: Partial<HabitInput>) => void;
  setHabitArchived: (id: string, archived: boolean) => void;
  deleteHabit: (id: string) => void;
  toggleHabitCheck: (habitId: string, date?: string) => boolean;

  // journal
  saveJournal: (entry: JournalEntry) => void;
}

const now = () => new Date().toISOString();

/** 各实体独立持久化；延迟写用于高频操作（打卡），保持各 key 之间最终一致。 */
const persist = (
  patch: Partial<Pick<AppState, "projects" | "tasks" | "captures" | "habits" | "habitChecks" | "journals" | "settings">>,
  immediate = true,
) => {
  const map: Record<string, string> = {
    projects: STORAGE_KEYS.projects, tasks: STORAGE_KEYS.tasks, captures: STORAGE_KEYS.captures,
    habits: STORAGE_KEYS.habits, habitChecks: STORAGE_KEYS.habitChecks, journals: STORAGE_KEYS.journals,
    settings: STORAGE_KEYS.settings,
  };
  for (const key of Object.keys(patch) as (keyof typeof map)[]) {
    const storageKey = map[key];
    if (!storageKey) continue;
    const value = patch[key as keyof typeof patch];
    if (immediate) writeJSON(storageKey, value);
    else writeJSONDeferred(storageKey, value);
  }
  if (immediate) flushWrites();
};

export const useAppStore = create<AppState>((set, get) => ({
  projects: [], tasks: [], captures: [], habits: [], habitChecks: [], journals: [],
  settings: DEFAULT_SETTINGS, hydrated: false,

  hydrate: () => {
    if (get().hydrated) return;
    set({
      projects: readJSON<Project[]>(STORAGE_KEYS.projects, []),
      tasks: readJSON<Task[]>(STORAGE_KEYS.tasks, []),
      captures: readJSON<Capture[]>(STORAGE_KEYS.captures, []),
      habits: readJSON<Habit[]>(STORAGE_KEYS.habits, []),
      habitChecks: readJSON<HabitCheck[]>(STORAGE_KEYS.habitChecks, []),
      journals: readJSON<JournalEntry[]>(STORAGE_KEYS.journals, []),
      settings: { ...DEFAULT_SETTINGS, ...readJSON<Partial<Settings>>(STORAGE_KEYS.settings, {}) },
      hydrated: true,
    });
  },

  resetAll: () => {
    set({ projects: [], tasks: [], captures: [], habits: [], habitChecks: [], journals: [], settings: DEFAULT_SETTINGS });
    persist({
      projects: [], tasks: [], captures: [], habits: [], habitChecks: [], journals: [], settings: DEFAULT_SETTINGS,
    });
  },

  replaceAll: (data) => {
    const settings = { ...DEFAULT_SETTINGS, ...(data.settings ?? {}) };
    set({
      projects: data.projects, tasks: data.tasks, captures: data.captures, habits: data.habits,
      habitChecks: data.habitChecks, journals: data.journals, settings,
    });
    persist({
      projects: data.projects, tasks: data.tasks, captures: data.captures, habits: data.habits,
      habitChecks: data.habitChecks, journals: data.journals, settings,
    });
  },

  setThemeMode: (themeMode) => {
    const settings = { ...get().settings, themeMode };
    set({ settings }); persist({ settings });
  },
  setLanguage: (language) => {
    const settings = { ...get().settings, language };
    set({ settings }); persist({ settings });
  },
  setAccentColor: (accentHue, accentSat = null, accentLight = null) => {
    const settings = { ...get().settings, accentHue, accentSat, accentLight };
    set({ settings }); persist({ settings });
  },
  markBackedUp: () => {
    const settings = { ...get().settings, lastBackupAt: Date.now() };
    set({ settings }); persist({ settings });
  },

  // ── 项目 ──
  createProject: (input) => {
    const ts = now();
    const project: Project = {
      id: createId(), name: input.name.trim(), emoji: input.emoji ?? "",
      archived: false, sortOrder: get().projects.length, createdAt: ts, updatedAt: ts,
    };
    const projects = [project, ...get().projects];
    set({ projects }); persist({ projects });
    return project;
  },
  updateProject: (id, input) => {
    const projects = get().projects.map((p) =>
      p.id === id ? { ...p, ...input, name: input.name?.trim() ?? p.name, updatedAt: now() } : p);
    set({ projects }); persist({ projects });
  },
  setProjectArchived: (id, archived) => {
    const projects = get().projects.map((p) => (p.id === id ? { ...p, archived, updatedAt: now() } : p));
    set({ projects }); persist({ projects });
  },
  deleteProjectPermanently: (id) => {
    // 任务不删除，只是变成"未归类"（对齐 Kotlin 的 SET_NULL）
    const projects = get().projects.filter((p) => p.id !== id);
    const tasks = get().tasks.map((t) => (t.projectId === id ? { ...t, projectId: null, updatedAt: now() } : t));
    set({ projects, tasks }); persist({ projects, tasks });
  },

  // ── 任务 ──
  createTask: (input) => {
    const ts = now();
    const task: Task = {
      id: createId(), title: input.title.trim(), notes: input.notes?.trim() ?? "",
      projectId: input.projectId ?? null, done: false, dueDate: input.dueDate ?? null,
      isMit: input.isMit ?? false, repeatRule: input.repeatRule ?? null,
      completedAt: null, createdAt: ts, updatedAt: ts,
    };
    const tasks = [task, ...get().tasks];
    set({ tasks }); persist({ tasks });
    return task;
  },
  updateTask: (id, input) => {
    const tasks = get().tasks.map((t) =>
      t.id === id ? { ...t, ...input, title: input.title?.trim() ?? t.title, updatedAt: now() } : t);
    set({ tasks }); persist({ tasks });
  },
  toggleTask: (id, todayStr) => {
    const tasks = get().tasks;
    const task = tasks.find((t) => t.id === id);
    if (!task) return;
    const ts = todayStr ?? todayKey();
    const completing = !task.done;
    let next = tasks.map((t) =>
      t.id === id ? { ...t, done: completing, completedAt: completing ? now() : null, updatedAt: now() } : t);

    if (task.repeatRule) {
      const on = nextOccurrence(task, ts);
      if (on && completing) {
        // 避免重复生成：若已存在同锚点的未完成副本，不再新建
        const exists = findGeneratedOccurrence(next, task, on);
        if (!exists) next = [makeNextTask(task, on, now(), createId()), ...next];
      } else if (on && !completing) {
        const generated = findGeneratedOccurrence(next, task, on);
        if (generated) next = next.filter((t) => t.id !== generated.id);
      }
    }
    set({ tasks: next }); persist({ tasks: next });
  },
  deleteTask: (id) => {
    const tasks = get().tasks.filter((t) => t.id !== id);
    set({ tasks }); persist({ tasks });
  },
  rescheduleOverdue: (todayStr) => {
    const ts = todayStr ?? todayKey();
    const overdue = get().tasks.filter((t) => !t.done && t.dueDate !== null && t.dueDate < ts);
    const moved: RescheduledTask[] = overdue.map((t) => ({ id: t.id, previousDueDate: t.dueDate }));
    const tasks = get().tasks.map((t) =>
      overdue.some((o) => o.id === t.id) ? { ...t, dueDate: ts, updatedAt: now() } : t);
    set({ tasks }); persist({ tasks });
    return moved;
  },
  undoReschedule: (items) => {
    const ids = new Set(items.map((i) => i.id));
    const tasks = get().tasks.map((t) => {
      const found = items.find((i) => i.id === t.id);
      return ids.has(t.id) && found ? { ...t, dueDate: found.previousDueDate, updatedAt: now() } : t;
    });
    set({ tasks }); persist({ tasks });
  },

  // ── 快速记录 ──
  capture: (text) => {
    const item: Capture = { id: createId(), text: text.trim(), processed: false, createdAt: now() };
    const captures = [item, ...get().captures];
    set({ captures }); persist({ captures });
    return item;
  },
  convertCapture: (id, projectId) => {
    const item = get().captures.find((c) => c.id === id);
    if (!item || item.processed) return null;
    const task = get().createTask({ title: item.text, projectId: projectId ?? null });
    const captures = get().captures.map((c) => (c.id === id ? { ...c, processed: true } : c));
    set({ captures }); persist({ captures });
    return task;
  },
  deleteCapture: (id) => {
    const captures = get().captures.filter((c) => c.id !== id);
    set({ captures }); persist({ captures });
  },

  // ── 习惯 ──
  createHabit: (input) => {
    const habit: Habit = {
      id: createId(), name: input.name.trim(), emoji: input.emoji ?? "",
      archived: false, sortOrder: get().habits.length, createdAt: now(),
    };
    const habits = [habit, ...get().habits];
    set({ habits }); persist({ habits });
    return habit;
  },
  updateHabit: (id, input) => {
    const habits = get().habits.map((h) => (h.id === id ? { ...h, ...input, name: input.name?.trim() ?? h.name } : h));
    set({ habits }); persist({ habits });
  },
  setHabitArchived: (id, archived) => {
    const habits = get().habits.map((h) => (h.id === id ? { ...h, archived } : h));
    set({ habits }); persist({ habits });
  },
  deleteHabit: (id) => {
    const habits = get().habits.filter((h) => h.id !== id);
    const habitChecks = get().habitChecks.filter((c) => c.habitId !== id);
    set({ habits, habitChecks }); persist({ habits, habitChecks });
  },
  toggleHabitCheck: (habitId, date) => {
    const d = date ?? todayKey();
    const checks = get().habitChecks;
    const existing = checks.find((c) => c.habitId === habitId && c.date === d);
    let habitChecks: HabitCheck[];
    let added: boolean;
    if (existing) {
      habitChecks = checks.filter((c) => c.id !== existing.id);
      added = false;
    } else {
      habitChecks = [...checks, { id: createId(), habitId, date: d, createdAt: now() }];
      added = true;
    }
    set({ habitChecks }); persist({ habitChecks });
    return added;
  },

  // ── 复盘 ──
  saveJournal: (entry) => {
    const journals = get().journals;
    const exists = journals.some((j) => j.date === entry.date);
    let next: JournalEntry[];
    if (isJournalEmpty(entry)) {
      next = journals.filter((j) => j.date !== entry.date); // 清空 = 删行
    } else if (exists) {
      next = journals.map((j) => (j.date === entry.date ? { ...entry, updatedAt: now() } : j));
    } else {
      const ts = now();
      next = [{ ...entry, createdAt: entry.createdAt || ts, updatedAt: ts }, ...journals];
    }
    set({ journals: next }); persist({ journals: next });
  },
}));
