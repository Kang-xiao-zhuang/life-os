"use client";

import { useEffect, useLayoutEffect } from "react";
import { usePathname } from "next/navigation";
import { useAppStore } from "@/store/app.store";
import { useTheme } from "@/lib/use-theme";
import { BottomNav } from "./bottom-nav";

const NAV_ROUTES = ["/", "/projects", "/capture", "/habits", "/journal"];

const useIsomorphicLayoutEffect = typeof window !== "undefined" ? useLayoutEffect : useEffect;

export function AppShell({ children }: { children: React.ReactNode }) {
  const hydrate = useAppStore((s) => s.hydrate);
  const pathname = usePathname();
  useTheme();

  useIsomorphicLayoutEffect(() => {
    hydrate();
  }, [hydrate]);

  const showNav = NAV_ROUTES.some((r) => pathname === r || pathname.startsWith(`${r}/`));

  return (
    <div className="relative mx-auto min-h-screen max-w-app bg-background shadow-sm">
      <div className={showNav ? "pb-28" : ""}>{children}</div>
      {showNav && <BottomNav />}
    </div>
  );
}
