"use client";

import { useEffect, useState, useRef } from "react";
import { Pencil } from "lucide-react";
import { cn } from "@/lib/utils";

export function EditableCell({
  value,
  placeholder,
  onSave,
  onCellClick,
  className,
  multiline,
}: {
  value: string;
  placeholder?: string;
  onSave: (val: string) => void;
  onCellClick?: () => void;
  className?: string;
  multiline?: boolean;
}) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(value);
  const ref = useRef<HTMLInputElement | HTMLTextAreaElement>(null);

  useEffect(() => {
    setDraft(value);
  }, [value]);

  useEffect(() => {
    if (editing) ref.current?.focus();
  }, [editing]);

  const handleSave = () => {
    setEditing(false);
    if (draft !== value) {
      onSave(draft);
    }
  };

  const handleCancel = () => {
    setEditing(false);
    setDraft(value);
  };

  if (editing) {
    const commonProps = {
      value: draft,
      onChange: (
        e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
      ) => setDraft(e.target.value),
      onBlur: handleSave,
      onKeyDown: (e: React.KeyboardEvent) => {
        if (e.key === "Enter" && !multiline) handleSave();
        if (e.key === "Escape") handleCancel();
      },
      className: cn(
        "w-full h-full px-2 py-1.5 rounded-md text-xs",
        "bg-background border border-primary/40",
        "focus:outline-none focus:ring-2 focus:ring-primary/30",
        "shadow-sm",
        className
      ),
      placeholder,
    };

    if (multiline) {
      return (
        <textarea
          ref={ref as React.RefObject<HTMLTextAreaElement>}
          rows={3}
          {...commonProps}
        />
      );
    }
    return (
      <input
        ref={ref as React.RefObject<HTMLInputElement>}
        type="text"
        {...commonProps}
      />
    );
  }

  return (
    <div
      onClick={(e) => {
        onCellClick?.();
        e.stopPropagation();
        setEditing(true);
      }}
      className={cn(
        // 撑满整个单元格
        "relative w-full h-full min-h-[32px] flex items-center",
        // hover 效果
        "px-2 py-1.5 rounded-md cursor-pointer",
        "border border-transparent",
        "hover:bg-muted/40 hover:border-border/40",
        "group/cell transition-all duration-150",
        // 字体
        "text-xs leading-relaxed",
        !value && "text-muted-foreground/30 italic",
        className
      )}
      title="点击编辑"
    >
      <span className="flex-1 whitespace-pre-wrap break-words">{value || placeholder || "\u00a0"}</span>
      <div className="absolute right-1 top-1/2 -translate-y-1/2 h-6 w-6 rounded-lg flex items-center justify-center invisible group-hover/cell:visible backdrop-blur-xl bg-white/70 shadow-sm z-10">
        <Pencil className="h-3.5 w-3.5 text-primary" />
      </div>
    </div>
  );
}
