"use client";

import { useEffect } from "react";
import { X } from "lucide-react";
import { useT } from "@/lib/i18n";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
}

/** 通用底部弹层（移动端友好）。 */
export function Modal({ open, onClose, title, children, footer }: ModalProps) {
  const { t } = useT();
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center" role="dialog" aria-modal="true" aria-label={title}>
      <div className="absolute inset-0 bg-black/35 backdrop-blur-[2px]" onClick={onClose} />
      <div className="animate-fade-up relative w-full max-w-app rounded-t-modal bg-surface px-5 pb-[max(1.5rem,env(safe-area-inset-bottom))] pt-4 shadow-modal">
        <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-border" />
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-text-primary">{title}</h2>
          <button onClick={onClose} aria-label={t("common.close")} className="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary hover:bg-border/50">
            <X size={20} />
          </button>
        </div>
        <div className="max-h-[65vh] overflow-y-auto">{children}</div>
        {footer && <div className="mt-5">{footer}</div>}
      </div>
    </div>
  );
}

interface ConfirmProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: React.ReactNode;
  danger?: boolean;
}

export function ConfirmDialog({ open, onClose, onConfirm, title, message, danger }: ConfirmProps) {
  const { t } = useT();
  return (
    <Modal open={open} onClose={onClose} title={title} footer={
      <div className="flex gap-3">
        <button onClick={onClose} className="h-11 flex-1 rounded-btn border border-border text-sm font-medium text-text-secondary">{t("common.cancel")}</button>
        <button onClick={() => { onConfirm(); onClose(); }} className={`h-11 flex-1 rounded-btn text-sm font-semibold text-white ${danger ? "bg-danger" : "bg-primary"}`}>
          {t("common.confirm")}
        </button>
      </div>
    }>
      <p className="text-sm leading-relaxed text-text-primary">{message}</p>
    </Modal>
  );
}
