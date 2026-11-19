// Single choke-point for LocalStorage — no component/store touches localStorage directly.
// Keys per docs/07-DATA-MODEL.md.

export const STORAGE_KEYS = {
  projects: "lifeos-projects",
  tasks: "lifeos-tasks",
  captures: "lifeos-captures",
  habits: "lifeos-habits",
  habitChecks: "lifeos-habit-checks",
  journals: "lifeos-journals",
  settings: "lifeos-settings",
  lastExport: "lifeos-last-export",
  snapshot: "lifeos-snapshot",
} as const;

// 延迟写:打卡记录可能有几千条,每点一次都整串 JSON 写回会卡。
// 连续操作合并成一次写入;App 切到后台 / 页面关闭时立即落盘。
const WRITE_DELAY = 300;
const pending = new Map<string, unknown>();
let timer: ReturnType<typeof setTimeout> | null = null;
let listening = false;

// --- 写入失败可观测 -------------------------------------------------------
// localStorage 配额满 / 隐私模式拒绝写入时,以前是静默吞掉:UI 显示打卡成功,
// 实际只存在内存里,切后台即丢失。现在标记一次,由 AppShell 弹醒目告警。
let writeFailed = false;
const errorListeners = new Set<() => void>();

export function hasWriteError(): boolean {
  return writeFailed;
}

/** 供 useSyncExternalStore 订阅;返回取消订阅函数。 */
export function subscribeWriteError(cb: () => void): () => void {
  errorListeners.add(cb);
  return () => { errorListeners.delete(cb); };
}

/** 用户已知晓后收起告警(不清除底层失败事实之外的状态)。 */
export function clearWriteError(): void {
  if (!writeFailed) return;
  writeFailed = false;
  errorListeners.forEach((l) => l());
}

function markWriteError(): void {
  if (writeFailed) return;
  writeFailed = true;
  errorListeners.forEach((l) => l());
}

/** 各主流浏览器 localStorage 配额约 5MB(Chromium 按 UTF-16 计 2 字节/字符)。 */
export const STORAGE_QUOTA = 5 * 1024 * 1024;

/** 统计本 App 各 key 的实际占用,用于设置页显示容量水位。 */
export function storageUsage(): number {
  if (typeof window === "undefined") return 0;
  let bytes = 0;
  try {
    for (const key of Object.values(STORAGE_KEYS)) {
      const raw = window.localStorage.getItem(key);
      if (raw) bytes += (key.length + raw.length) * 2;
    }
  } catch {
    // 读不到就返回已累计的值,不让设置页因此崩溃
  }
  return bytes;
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
}

export function flushWrites(): void {
  if (timer) clearTimeout(timer);
  timer = null;
  const entries = [...pending];
  pending.clear();
  for (const [key, value] of entries) writeJSON(key, value);
}

export function writeJSONDeferred<T>(key: string, value: T): void {
  if (typeof window === "undefined") return;
  if (!listening) {
    listening = true;
    window.addEventListener("pagehide", flushWrites);
    document.addEventListener("visibilitychange", () => {
      if (document.visibilityState === "hidden") flushWrites();
    });
  }
  pending.set(key, value);
  timer ??= setTimeout(flushWrites, WRITE_DELAY);
}

export function readJSON<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;
  if (pending.has(key)) return pending.get(key) as T; // 尚未落盘的最新值
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

/** 直接写盘。返回是否成功;失败会触发全局告警(组件可用 hasWriteError 订阅)。 */
export function writeJSON<T>(key: string, value: T): boolean {
  if (typeof window === "undefined") return true; // SSR:没有存储,不算失败
  pending.delete(key); // 直接写(如导入备份)覆盖排队中的旧值,避免稍后被冲掉
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
    return true;
  } catch {
    // storage full / unavailable —— 状态仍在内存里,但必须让用户立刻知道
    markWriteError();
    return false;
  }
}
