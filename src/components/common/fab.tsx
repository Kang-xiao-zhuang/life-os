"use client";

import { Plus } from "lucide-react";

export function Fab({ onClick, label }: { onClick: () => void; label: string }) {
  return (
    <button
      onClick={onClick}
      aria-label={label}
      className="fixed bottom-24 right-[max(1rem,calc(50%-224px+1rem))] z-30 flex h-14 items-center gap-2 rounded-full bg-primary px-5 font-semibold text-background shadow-lg transition-transform active:scale-95"
    >
      <Plus size={22} strokeWidth={2.4} />
      {label}
    </button>
  );
}
