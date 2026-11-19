import { describe, expect, it } from "vitest";
import { dateKey, addDays, addMonths, diffDays, weekDays, monthDays, shiftMonth } from "@/utils/date";

describe("date utils (TZ=Asia/Shanghai)", () => {
  it("dayKey 按本地日期", () => expect(dateKey(new Date("2026-11-02T23:30:00Z"))).toBe("2026-11-03"));
  it("addDays 跨月", () => expect(addDays("2026-11-30", 1)).toBe("2026-12-01"));
  it("addMonths 月末 clamp 到 28", () => expect(addMonths("2026-01-31", 1)).toBe("2026-02-28"));
  it("diffDays 正负", () => {
    expect(diffDays("2026-11-20", "2026-11-18")).toBe(2);
    expect(diffDays("2026-11-18", "2026-11-20")).toBe(-2);
  });
  it("weekDays 周一到周日", () => {
    const w = weekDays("2026-11-19"); // 周四
    expect(w[0]).toBe("2026-11-16"); // 周一
    expect(w[6]).toBe("2026-11-22");
  });
  it("monthDays 平年2月 28 天", () => expect(monthDays("2026-02")).toHaveLength(28));
  it("shiftMonth 跨年", () => expect(shiftMonth("2026-01", -1)).toBe("2025-12"));
});
