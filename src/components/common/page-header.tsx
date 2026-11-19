"use client";

import { useRouter } from "next/navigation";
import { ChevronLeft } from "lucide-react";

interface PageHeaderProps {
  title: string;
  back?: boolean;
  action?: React.ReactNode;
}

export function PageHeader({ title, back, action }: PageHeaderProps) {
  const router = useRouter();
  return (
    <header className="sticky top-0 z-30 flex items-center gap-2 border-b border-border/60 bg-background/90 px-4 py-3 backdrop-blur">
      {back && (
        <button
          aria-label="返回"
          onClick={() => router.back()}
          className="-ml-2 flex h-9 w-9 items-center justify-center rounded-full text-text-primary hover:bg-border/50"
        >
          <ChevronLeft size={24} />
        </button>
      )}
      <h1 className="flex-1 truncate text-xl font-bold text-text-primary">{title}</h1>
      {action}
    </header>
  );
}
