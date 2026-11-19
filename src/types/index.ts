// LifeOS 数据模型 —— 对齐 Kotlin 的 entity/model，日期统一用本地 "YYYY-MM-DD" 字符串。
// 存储层只放纯数据，所有统计（streak / 热力图 / 进度）实时推导，不存冗余。

export type RepeatRule = "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY";
export type ThemeMode = "system" | "light" | "dark";
export type AppLanguage = "system" | "zh" | "en";

/** 项目 = 长期在做的事。 */
export interface Project {
  id: string;
  name: string;
  emoji: string;
  archived: boolean;
  sortOrder: number;
  createdAt: string; // ISO timestamp
  updatedAt: string;
}

/** 任务。dueDate 为 "YYYY-MM-DD" 或 null。 */
export interface Task {
  id: string;
  title: string;
  notes: string;
  projectId: string | null;
  done: boolean;
  dueDate: string | null;
  isMit: boolean;
  repeatRule: RepeatRule | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** 快速记录（收件箱）。 */
export interface Capture {
  id: string;
  text: string;
  processed: boolean;
  createdAt: string;
}

/** 习惯。 */
export interface Habit {
  id: string;
  name: string;
  emoji: string;
  archived: boolean;
  sortOrder: number;
  createdAt: string;
}

/** 习惯打卡：habitId + date 唯一。 */
export interface HabitCheck {
  id: string;
  habitId: string;
  date: string; // YYYY-MM-DD
  createdAt: string;
}

/** 每日复盘（一天一条）。 */
export interface JournalEntry {
  date: string; // YYYY-MM-DD，唯一
  done: string;
  win: string;
  problems: string;
  tomorrowMit: string;
  createdAt: string;
  updatedAt: string;
}

export interface Settings {
  themeMode: ThemeMode;
  language: AppLanguage;
  lastBackupAt: number; // epoch ms，0 = 从未
}

/** 逾期批量挪期撤销用。 */
export interface RescheduledTask {
  id: string;
  previousDueDate: string | null;
}

/** 备份文件结构。 */
export interface BackupData {
  app: "lifeos";
  schemaVersion: number;
  exportedAt: string;
  versionName: string;
  projects: Project[];
  tasks: Task[];
  captures: Capture[];
  habits: Habit[];
  habitChecks: HabitCheck[];
  journals: JournalEntry[];
  settings?: Partial<Settings>;
}

export const SCHEMA_VERSION = 2;
export const EMPTY_JOURNAL = (date: string): JournalEntry => ({
  date, done: "", win: "", problems: "", tomorrowMit: "",
  createdAt: "", updatedAt: "",
});

export const isJournalEmpty = (j: JournalEntry): boolean =>
  !j.done.trim() && !j.win.trim() && !j.problems.trim() && !j.tomorrowMit.trim();

export const JOURNAL_FIELDS = ["done", "win", "problems", "tomorrowMit"] as const;
