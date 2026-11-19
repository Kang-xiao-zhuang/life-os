import { describe, expect, it } from "vitest";
import type { Task, Habit, HabitCheck } from "@/types";
import {
  advanceRepeat, nextOccurrence, makeNextTask, findGeneratedOccurrence,
  habitStreak, habitWeek, habitMonth, isOverdue, sortOpenTasks, appendCompletedLines, dashboardData, activityDays,
} from "@/utils/business";
import { addDays, addMonths, dateKey } from "@/utils/date";

const task = (over: Partial<Task> = {}): Task => ({
  id: "t1", title: "周报", notes: "", projectId: null, done: false, dueDate: "2026-11-16",
  isMit: false, repeatRule: "WEEKLY", completedAt: null, createdAt: "2026-11-01T00:00:00.000Z",
  updatedAt: "2026-11-01T00:00:00.000Z", ...over,
});

describe("date 月末 clamp", () => {
  it("1月31日 +1月 = 2月28日（平年）", () => expect(addMonths("2026-01-31", 1)).toBe("2026-02-28"));
  it("闰日 +1年 = 2月28日", () => expect(addMonths("2024-02-29", 12)).toBe("2025-02-28"));
});

describe("重复任务", () => {
  it("按 dueDate 锚定，晚一天勾选仍回到原星期", () => {
    expect(nextOccurrence(task({ dueDate: "2026-11-16" }), "2026-11-20")).toBe("2026-11-23");
  });
  it("无 dueDate 回退到 today", () => {
    expect(nextOccurrence(task({ dueDate: null }), "2026-11-20")).toBe("2026-11-27");
  });
  it("下一条不继承 MIT、done", () => {
    const src = task({ isMit: true });
    const next = makeNextTask(src, "2026-11-23", "2026-11-20T00:00:00Z", "t2");
    expect(next.isMit).toBe(false);
    expect(next.done).toBe(false);
    expect(next.completedAt).toBeNull();
  });
  it("取消勾选时只收回未编辑未勾选的副本", () => {
    const src = task();
    const gen = makeNextTask(src, "2026-11-23", "2026-11-20T00:00:00Z", "t2");
    const edited = { ...gen, title: "改过的" };
    expect(findGeneratedOccurrence([edited], src, "2026-11-23")?.id).toBeUndefined();
    expect(findGeneratedOccurrence([gen], src, "2026-11-23")?.id).toBe("t2");
  });
});

describe("习惯 streak", () => {
  const habitId = "h1";
  const check = (date: string): HabitCheck => ({ id: date, habitId, date, createdAt: "" });
  it("今天没打卡不算断，从昨天起量", () => {
    const checks = [check("2026-11-18"), check("2026-11-19")];
    expect(habitStreak(checks, habitId, "2026-11-20")).toBe(2);
  });
  it("今天打了接上", () => {
    const checks = [check("2026-11-19"), check("2026-11-20")];
    expect(habitStreak(checks, habitId, "2026-11-20")).toBe(2);
  });
  it("断一天就归零", () => {
    expect(habitStreak([check("2026-11-18")], habitId, "2026-11-20")).toBe(0);
  });
  it("周网格周一开头", () => {
    const week = habitWeek([check("2026-11-16")], habitId, "2026-11-19");
    expect(week).toHaveLength(7);
    expect(week[0]).toBe(true); // 11-16 是周一
  });
  it("热力图完成度按活跃习惯数做分母", () => {
    const h: Habit = { id: habitId, name: "读书", emoji: "", archived: false, sortOrder: 0, createdAt: "" };
    const cells = habitMonth([check("2026-11-19")], 2, ["2026-11-19", "2026-11-21"], "2026-11-20");
    expect(cells[0].completion).toBeCloseTo(0.5);
    expect(cells[1].completion).toBeNull(); // 未来
  });
});

describe("任务排序与逾期", () => {
  it("逾期在前、今天其次、无日期最后", () => {
    const list = [
      task({ id: "nodate", dueDate: null }),
      task({ id: "later", dueDate: "2026-11-25" }),
      task({ id: "over", dueDate: "2026-11-15" }),
      task({ id: "today", dueDate: "2026-11-20" }),
    ];
    expect(sortOpenTasks(list, "2026-11-20").map((t) => t.id)).toEqual(["over", "today", "later", "nodate"]);
  });
  it("已完成不算逾期", () => {
    expect(isOverdue(task({ done: true, dueDate: "2026-11-01" }), "2026-11-20")).toBe(false);
  });
});

describe("Dashboard 聚合", () => {
  it("MIT 不重复出现在今日任务，逾期独立分组", () => {
    const mit = task({ id: "mit", isMit: true, dueDate: "2026-11-20" });
    const due = task({ id: "due", dueDate: "2026-11-20" });
    const over = task({ id: "over", dueDate: "2026-11-15" });
    const d = dashboardData([mit, due, over], [], [], [], [], "2026-11-20");
    expect(d.mit.map((t) => t.id)).toEqual(["mit"]);
    expect(d.dueToday.map((t) => t.id)).toEqual(["due"]);
    expect(d.overdue.map((t) => t.id)).toEqual(["over"]);
  });
  it("带出昨日复盘的明日 MIT", () => {
    const yJournal = { date: "2026-11-19", done: "", win: "", problems: "", tomorrowMit: "写完方案", createdAt: "", updatedAt: "" };
    const d = dashboardData([], [], [], [yJournal], [], "2026-11-20");
    expect(d.carriedMit).toBe("写完方案");
  });
});

describe("复盘带出已完成", () => {
  it("追加并去重（忽略 - 前缀）", () => {
    expect(appendCompletedLines("- 读书\n- 跑步", ["读书", "写代码"])).toBe("- 读书\n- 跑步\n- 写代码");
  });
});

describe("近 30 天活跃", () => {
  it("按本地日期合并任务完成与习惯打卡，只保留窗口内日期", () => {
    const done = task({ id: "done", done: true, completedAt: "2026-11-20T00:00:00Z" });
    const old = task({ id: "old", done: true, completedAt: "2026-10-01T00:00:00Z" });
    const checks: HabitCheck[] = [{ id: "c1", habitId: "h1", date: "2026-11-20", createdAt: "" }];
    const points = activityDays([done, old], checks, "2026-11-20", 30);
    expect(points).toHaveLength(30);
    expect(points[points.length - 1]).toEqual({ date: "11-20", count: 2 });
    expect(points.reduce((s, p) => s + p.count, 0)).toBe(2);
  });
  it("今日统计纳入完成任务、习惯与复盘", () => {
    const done = task({ id: "done", done: true, completedAt: "2026-11-20T00:00:00Z" });
    const h: Habit = { id: "h1", name: "读书", emoji: "", archived: false, sortOrder: 0, createdAt: "" };
    const check: HabitCheck = { id: "c1", habitId: "h1", date: "2026-11-20", createdAt: "" };
    const j = { date: "2026-11-20", done: "写了方案", win: "", problems: "", tomorrowMit: "", createdAt: "", updatedAt: "" };
    const d = dashboardData([done], [h], [check], [j], [], "2026-11-20");
    expect(d.todayDone).toBe(1);
    expect(d.todayHabits).toBe(1);
    expect(d.hasJournal).toBe(true);
  });
});
