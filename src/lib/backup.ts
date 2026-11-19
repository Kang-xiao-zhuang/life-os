import { z } from "zod";
import type { BackupData, Task, Project, Habit, HabitCheck, Capture, JournalEntry, Settings } from "@/types";
import { SCHEMA_VERSION } from "@/types";

const taskSchema = z.object({
  id: z.string(), title: z.string(), notes: z.string(), projectId: z.string().nullable(),
  done: z.boolean(), dueDate: z.string().nullable(), isMit: z.boolean(),
  repeatRule: z.enum(["DAILY", "WEEKLY", "MONTHLY", "YEARLY"]).nullable(),
  completedAt: z.string().nullable(), createdAt: z.string(), updatedAt: z.string(),
});
const projectSchema = z.object({
  id: z.string(), name: z.string(), emoji: z.string(), archived: z.boolean(),
  sortOrder: z.number(), createdAt: z.string(), updatedAt: z.string(),
});
const habitSchema = z.object({
  id: z.string(), name: z.string(), emoji: z.string(), archived: z.boolean(), sortOrder: z.number(), createdAt: z.string(),
});
const checkSchema = z.object({ id: z.string(), habitId: z.string(), date: z.string(), createdAt: z.string() });
const captureSchema = z.object({ id: z.string(), text: z.string(), processed: z.boolean(), createdAt: z.string() });
const journalSchema = z.object({
  date: z.string(), done: z.string(), win: z.string(), problems: z.string(), tomorrowMit: z.string(),
  createdAt: z.string(), updatedAt: z.string(),
});

export const backupSchema = z.object({
  app: z.literal("lifeos"),
  schemaVersion: z.number(),
  exportedAt: z.string(),
  versionName: z.string(),
  projects: z.array(projectSchema),
  tasks: z.array(taskSchema),
  captures: z.array(captureSchema),
  habits: z.array(habitSchema),
  habitChecks: z.array(checkSchema),
  journals: z.array(journalSchema),
  settings: z.object({
    themeMode: z.enum(["system", "light", "dark"]).optional(),
    language: z.enum(["system", "zh", "en"]).optional(),
  }).passthrough().optional(),
});

export class BackupError extends Error {}

/** 严格校验 + schemaVersion 对齐。 */
export function parseBackup(text: string): BackupData {
  let raw: unknown;
  try { raw = JSON.parse(text); } catch { throw new BackupError("invalid"); }
  const parsed = backupSchema.safeParse(raw);
  if (!parsed.success) throw new BackupError("invalid");
  const data = parsed.data;
  if (data.schemaVersion !== SCHEMA_VERSION) {
    throw new BackupError(`schema:${data.schemaVersion}`);
  }
  return data as BackupData;
}

export function buildBackup(input: {
  projects: Project[]; tasks: Task[]; captures: Capture[]; habits: Habit[];
  habitChecks: HabitCheck[]; journals: JournalEntry[]; settings: Settings;
}, versionName: string): BackupData {
  return {
    app: "lifeos",
    schemaVersion: SCHEMA_VERSION,
    exportedAt: new Date().toISOString(),
    versionName,
    projects: input.projects,
    tasks: input.tasks,
    captures: input.captures,
    habits: input.habits,
    habitChecks: input.habitChecks,
    journals: input.journals,
    settings: { themeMode: input.settings.themeMode, language: input.settings.language },
  };
}

export interface BackupPreview {
  exportedAt: string;
  projects: number;
  tasks: number;
  habits: number;
  journals: number;
}

export function previewBackup(data: BackupData): BackupPreview {
  return {
    exportedAt: data.exportedAt,
    projects: data.projects.length,
    tasks: data.tasks.length,
    habits: data.habits.length,
    journals: data.journals.length,
  };
}

export function downloadBackup(data: BackupData) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  const day = new Date().toISOString().slice(0, 10);
  a.download = `LifeOS_Backup_${day}.json`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
