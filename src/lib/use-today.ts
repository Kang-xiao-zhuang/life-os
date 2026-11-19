"use client";

import { useEffect, useState } from "react";
import { today as todayKey } from "@/utils/date";

/** 返回当天 dateKey，并在跨午夜 / App 回到前台时刷新（避免页面停留在昨天的日期）。 */
export function useToday(): string {
  const [day, setDay] = useState<string>("");
  useEffect(() => {
    setDay(todayKey());
    const refresh = () => setDay(todayKey());
    const onVis = () => { if (document.visibilityState === "visible") refresh(); };
    document.addEventListener("visibilitychange", onVis);
    // 每分钟检查一次，覆盖 App 常驻前台跨午夜的情况
    const id = window.setInterval(() => setDay((prev) => {
      const now = todayKey();
      return now !== prev ? now : prev;
    }), 60_000);
    return () => { document.removeEventListener("visibilitychange", onVis); clearInterval(id); };
  }, []);
  return day;
}
