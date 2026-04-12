"use client";

import { useState } from "react";
import { X } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";
import { projectApi, type ProjectCreateReq } from "@/lib/api/project";

interface CreateProjectDialogProps {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}

export function CreateProjectDialog({
  open,
  onClose,
  onCreated,
}: CreateProjectDialogProps) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    if (!name.trim()) {
      setError("请输入项目名称");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const data: ProjectCreateReq = { name: name.trim() };
      if (description.trim()) data.description = description.trim();
      await projectApi.create(data);
      setName("");
      setDescription("");
      onCreated();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AnimatePresence>
      {open && (
        <>
          {/* 遮罩 */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
            onClick={onClose}
          />
          {/* 弹窗 */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ duration: 0.2 }}
            className="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 w-full max-w-md"
          >
            <div
              className={cn(
                "rounded-2xl border border-border/40 p-6",
                "bg-card shadow-2xl shadow-black/20"
              )}
            >
              {/* 标题 */}
              <div className="flex items-center justify-between mb-5">
                <h2 className="text-lg font-semibold">新建项目</h2>
                <button
                  onClick={onClose}
                  className="p-1.5 rounded-lg hover:bg-muted transition-colors"
                >
                  <X className="h-4 w-4 text-muted-foreground" />
                </button>
              </div>

              {/* 表单 */}
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1.5">
                    项目名称 <span className="text-destructive">*</span>
                  </label>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="例如：品牌宣传片"
                    className={cn(
                      "w-full px-3.5 py-2.5 rounded-xl text-sm",
                      "bg-muted/50 border border-border/40",
                      "focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50",
                      "placeholder:text-muted-foreground/50 transition-all"
                    )}
                    autoFocus
                    onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1.5">
                    项目描述
                  </label>
                  <textarea
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="简要描述项目内容和目标..."
                    rows={3}
                    className={cn(
                      "w-full px-3.5 py-2.5 rounded-xl text-sm resize-none",
                      "bg-muted/50 border border-border/40",
                      "focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50",
                      "placeholder:text-muted-foreground/50 transition-all"
                    )}
                  />
                </div>

                {error && (
                  <p className="text-sm text-destructive">{error}</p>
                )}
              </div>

              {/* 按钮 */}
              <div className="flex justify-end gap-3 mt-6">
                <button
                  onClick={onClose}
                  className={cn(
                    "px-4 py-2 rounded-xl text-sm font-medium",
                    "hover:bg-muted transition-colors"
                  )}
                >
                  取消
                </button>
                <button
                  onClick={handleSubmit}
                  disabled={loading}
                  className={cn(
                    "px-5 py-2 rounded-xl text-sm font-medium",
                    "bg-primary text-primary-foreground",
                    "hover:opacity-90 active:scale-[0.98] transition-all",
                    "disabled:opacity-50 disabled:cursor-not-allowed"
                  )}
                >
                  {loading ? "创建中..." : "创建项目"}
                </button>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
