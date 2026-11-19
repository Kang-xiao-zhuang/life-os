"use client";

import { useEffect, useState } from "react";
import { useT } from "@/lib/i18n";
import { Modal } from "./modal";
import { EmojiStrip } from "./emoji-strip";

interface Props {
  open: boolean;
  onClose: () => void;
  onSave: (name: string, emoji: string) => void;
  title: string;
  nameLabel: string;
  initialName?: string;
  initialEmoji?: string;
  kind?: "project" | "habit";
  extraAction?: { label: string; onClick: () => void };
}

export function NameEmojiDialog({ open, onClose, onSave, title, nameLabel, initialName = "", initialEmoji = "", kind = "project", extraAction }: Props) {
  const { t } = useT();
  const [name, setName] = useState(initialName);
  const [emoji, setEmoji] = useState(initialEmoji);

  // showNew 的弹层常驻挂载，打开时重置，避免上一次输入残留
  useEffect(() => {
    if (!open) return;
    setName(initialName);
    setEmoji(initialEmoji);
  }, [open, initialName, initialEmoji]);

  return (
    <Modal open={open} onClose={onClose} title={title} footer={
      <div className="flex gap-3">
        {extraAction && (
          <button onClick={() => { extraAction.onClick(); onClose(); }} className="h-11 shrink-0 rounded-btn border border-border px-4 text-sm font-medium text-danger">
            {extraAction.label}
          </button>
        )}
        <button onClick={() => { if (name.trim()) { onSave(name.trim(), emoji); onClose(); } }} disabled={!name.trim()}
          className="h-11 flex-1 rounded-btn bg-primary text-sm font-semibold text-background disabled:opacity-40">
          {t("common.save")}
        </button>
      </div>
    }>
      <div className="space-y-5">
        <div>
          <label className="mb-2 block text-xs font-medium text-text-secondary">{nameLabel}</label>
          <input autoFocus value={name} onChange={(e) => setName(e.target.value)} className="w-full rounded-input border border-border bg-background px-3.5 py-2.5 text-sm text-text-primary outline-none focus:border-primary" />
        </div>
        <div>
          <label className="mb-2 block text-xs font-medium text-text-secondary">{t("projects.emoji")}</label>
          <EmojiStrip value={emoji} onChange={setEmoji} />
        </div>
      </div>
    </Modal>
  );
}
