"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, FolderKanban, PlusCircle, Flame, BookOpen } from "lucide-react";
import { cn } from "@/lib/cn";
import { useT } from "@/lib/i18n";

const TABS = [
  { href: "/", key: "nav.home", Icon: Home },
  { href: "/projects", key: "nav.projects", Icon: FolderKanban },
  { href: "/capture", key: "nav.capture", Icon: PlusCircle, center: true },
  { href: "/habits", key: "nav.habits", Icon: Flame },
  { href: "/journal", key: "nav.journal", Icon: BookOpen },
] as const;

export function BottomNav() {
  const pathname = usePathname();
  const { t } = useT();
  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 mx-auto max-w-app border-t border-border bg-surface/95 backdrop-blur">
      <div className="flex items-stretch justify-around px-1.5 py-2">
        {TABS.map(({ href, key, Icon, ...rest }) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
          const label = t(key);
          const center = "center" in rest && rest.center;
          return (
            <Link
              key={href}
              href={href}
              aria-label={label}
              className={cn(
                "flex flex-1 flex-col items-center gap-1 rounded-btn py-1.5 text-[11px] font-medium transition-colors",
                center && "-mt-4",
                active ? "text-primary" : "text-text-secondary hover:text-text-primary",
              )}
            >
              <span className={cn("flex items-center justify-center rounded-full", center && "h-11 w-11 border border-border bg-surface shadow-sm")}>
                <Icon size={center ? 24 : 22} strokeWidth={active ? 2.4 : 2} />
              </span>
              {label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
