// 日期统一用本地 "YYYY-MM-DD" 字符串（等价 Kotlin 的 epoch-day，但可读、无时区坑）。
// 所有"今天"都通过参数传入，便于测试，也避免跨午夜后页面冻结在旧日期。

const pad = (n: number) => String(n).padStart(2, "0");

/** Date → "YYYY-MM-DD"（本地时区）。 */
export function dateKey(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** "YYYY-MM-DD" → Date（本地零点）。 */
export function parseDate(key: string): Date {
  const [y, m, d] = key.split("-").map(Number);
  return new Date(y, m - 1, d);
}

export function today(): string {
  return dateKey(new Date());
}

/** 加天数，返回新的 dateKey。 */
export function addDays(key: string, days: number): string {
  const d = parseDate(key);
  d.setDate(d.getDate() + days);
  return dateKey(d);
}

/** 加月/年，月末自动 clamp（1/31 → 2/28，闰日 → 2/28），对齐 kotlin plusMonths/plusYears。 */
export function addMonths(key: string, months: number): string {
  const d = parseDate(key);
  const day = d.getDate();
  d.setDate(1);
  d.setMonth(d.getMonth() + months);
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
  d.setDate(Math.min(day, last));
  return dateKey(d);
}

export function addYears(key: string, years: number): string {
  return addMonths(key, years * 12);
}

/** 两个 dateKey 相差天数（a - b）。 */
export function diffDays(a: string, b: string): number {
  return Math.round((parseDate(a).getTime() - parseDate(b).getTime()) / 86400000);
}

/** "M月D日 周X"。 */
export function formatDate(key: string, lang: "zh" | "en" = "zh"): string {
  const d = parseDate(key);
  if (lang === "en") {
    return d.toLocaleDateString("en-US", { month: "short", day: "numeric", weekday: "short" });
  }
  const week = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"][d.getDay()];
  return `${d.getMonth() + 1}月${d.getDate()}日 ${week}`;
}

/** 星期几短标签（一/二/…/日），周一开头。 */
export function weekdayShort(key: string): string {
  const d = parseDate(key).getDay();
  return ["日", "一", "二", "三", "四", "五", "六"][d];
}

/** 所在周的周一 dateKey。 */
export function weekStart(key: string): string {
  const d = parseDate(key);
  const dow = (d.getDay() + 6) % 7; // 周一=0
  return addDays(key, -dow);
}

/** 该周的 7 个 dateKey（周一→周日）。 */
export function weekDays(key: string): string[] {
  const start = weekStart(key);
  return Array.from({ length: 7 }, (_, i) => addDays(start, i));
}

/** 所在月的 "YYYY-MM"。 */
export function monthKey(key: string): string {
  return key.slice(0, 7);
}

/** 某月的全部日期 key。 */
export function monthDays(ym: string): string[] {
  const [y, m] = ym.split("-").map(Number);
  const count = new Date(y, m, 0).getDate();
  return Array.from({ length: count }, (_, i) => `${ym}-${pad(i + 1)}`);
}

/** 月份加减，返回 "YYYY-MM"。 */
export function shiftMonth(ym: string, delta: number): string {
  const [y, m] = ym.split("-").map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
}

/** "YYYY年M月"。 */
export function formatMonth(ym: string, lang: "zh" | "en" = "zh"): string {
  const [y, m] = ym.split("-").map(Number);
  if (lang === "en") return new Date(y, m - 1, 1).toLocaleDateString("en-US", { month: "long", year: "numeric" });
  return `${y}年${m}月`;
}

/** 时间 "HH:MM" + 日期 → epoch ms（用于跨日比较等）。 */
export function atTime(dateKeyStr: string, hhmm: string): number {
  const [h, m] = hhmm.split(":").map(Number);
  const d = parseDate(dateKeyStr);
  d.setHours(h, m, 0, 0);
  return d.getTime();
}
