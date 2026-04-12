"use client";

import { useEffect, useState, useCallback } from "react";
import {
  Bot,
  Settings2,
  Loader2,
  Plus,
  Trash2,
  Edit2,
  Eye,
  EyeOff,
  Star,
  ChevronRight,
} from "lucide-react";
import { motion } from "framer-motion";
import { cn } from "@/lib/utils";
import {
  aiModelApi,
  apiConfigApi,
  PLATFORM_OPTIONS,
  PLATFORM_LABELS,
  MODEL_TYPE_OPTIONS,
  MODEL_TYPE_LABELS,
  type AiModel,
  type ApiConfig,
  type AiModelCreateReq,
  type AiModelUpdateReq,
  type ApiConfigSaveReq,
  type ModelPreset,
} from "@/lib/api/ai-model";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectGroup,
  SelectItem,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Sparkles, Check } from "lucide-react";
import {
  containerVariants,
  itemVariants,
  platformIconColors,
  maskSecret,
  getPlatformFields,
  getPlatformDefaultUrl,
} from "../_shared";

// ============================================================
// API 配置 Dialog
// ============================================================

interface ApiConfigDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editingConfig: ApiConfig | null;
  onSaved: () => void;
}

function ApiConfigDialog({ open, onOpenChange, editingConfig, onSaved }: ApiConfigDialogProps) {
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<ApiConfigSaveReq>({ name: "" });
  const [showSecrets, setShowSecrets] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (open) {
      if (editingConfig) {
        setForm({
          id: editingConfig.id,
          name: editingConfig.name,
          platform: editingConfig.platform || "",
          apiUrl: editingConfig.apiUrl || "",
          apiKey: editingConfig.apiKey || "",
          appId: editingConfig.appId || "",
          appSecret: editingConfig.appSecret || "",
          status: editingConfig.status,
          remark: editingConfig.remark || "",
        });
      } else {
        setForm({ name: "", platform: "openai_compatible", apiUrl: "", apiKey: "", appId: "", appSecret: "", status: 1 });
      }
      setShowSecrets({});
    }
  }, [open, editingConfig]);

  const updateField = <K extends keyof ApiConfigSaveReq>(key: K, value: ApiConfigSaveReq[K]) => {
    setForm(prev => ({ ...prev, [key]: value }));
  };

  const handleSave = async () => {
    if (!form.name.trim()) return;
    setSaving(true);
    try {
      if (editingConfig) {
        await apiConfigApi.update(form);
      } else {
        await apiConfigApi.create(form);
      }
      onSaved();
      onOpenChange(false);
    } catch (err) {
      console.error("保存 API 配置失败:", err);
    } finally {
      setSaving(false);
    }
  };

  const fields = getPlatformFields(form.platform || "");

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{editingConfig ? "编辑 API 配置" : "新建 API 配置"}</DialogTitle>
          <DialogDescription>
            配置外部 AI 服务的 API 接入信息
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* 配置名称 */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">配置名称</Label>
            <Input
              placeholder="例如：DeepSeek / GPT-4o / Gemini"
              value={form.name}
              onChange={e => updateField("name", e.target.value)}
              className="text-sm"
            />
          </div>

          {/* 平台选择 */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">平台</Label>
            <Select
              value={form.platform || "openai_compatible"}
              onValueChange={v => {
                updateField("platform", v as string);
                // 切换平台时自动填入默认 URL
                const defaultUrl = getPlatformDefaultUrl(v as string);
                if (defaultUrl) {
                  updateField("apiUrl", defaultUrl);
                }
              }}
              items={PLATFORM_OPTIONS.map(o => ({ value: o.value, label: o.label }))}
            >
              <SelectTrigger className="w-full text-sm">
                <SelectValue placeholder="选择平台" />
              </SelectTrigger>
              <SelectContent className="text-sm">
                <SelectGroup>
                  {PLATFORM_OPTIONS.map(opt => (
                    <SelectItem key={opt.value} value={opt.value} className="text-sm">
                      <div>
                        <div>{opt.label}</div>
                        <div className="text-[10px] text-muted-foreground">{opt.description}</div>
                      </div>
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>

          {/* 动态平台字段 */}
          {fields.map(field => (
            <div key={field.key} className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">
                {field.label}
                {field.required && <span className="text-destructive ml-0.5">*</span>}
              </Label>
              <div className="relative">
                <Input
                  type={field.type === "password" && !showSecrets[field.key] ? "password" : "text"}
                  placeholder={field.placeholder}
                  value={(form as unknown as Record<string, string>)[field.key] || ""}
                  onChange={e => updateField(field.key as keyof ApiConfigSaveReq, e.target.value)}
                  className="text-sm pr-9"
                />
                {field.type === "password" && (
                  <button
                    type="button"
                    onClick={() => setShowSecrets(prev => ({ ...prev, [field.key]: !prev[field.key] }))}
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                  >
                    {showSecrets[field.key] ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                  </button>
                )}
              </div>
            </div>
          ))}

          {/* 备注 */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">备注</Label>
            <Input
              placeholder="可选备注信息"
              value={form.remark || ""}
              onChange={e => updateField("remark", e.target.value)}
              className="text-sm"
            />
          </div>
        </div>

        <DialogFooter>
          <DialogClose render={<Button variant="outline" size="sm" />}>
            取消
          </DialogClose>
          <Button size="sm" onClick={handleSave} disabled={saving || !form.name.trim()}>
            {saving && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />}
            {editingConfig ? "保存" : "创建"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ============================================================
// 模型配置可视化表单
// ============================================================

/** 解析 config JSON 字符串为对象，解析失败返回空对象 */
function parseConfigJson(json: string | undefined | null): Record<string, unknown> {
  if (!json) return {};
  try {
    const parsed = JSON.parse(json);
    return typeof parsed === "object" && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

/** 常见宽高比 */
const COMMON_ASPECT_RATIOS = ["1:1", "3:4", "4:3", "16:9", "9:16", "2:3", "3:2", "21:9"];

/** 常见分辨率档位 */
const COMMON_TIERS = ["1K", "2K", "3K", "4K"];

// ---------- supportedSizes 编辑器 ----------

type SizesMap = Record<string, Record<string, string>>;

function SupportedSizesEditor({
  value,
  onChange,
}: {
  value: SizesMap | undefined;
  onChange: (v: SizesMap | undefined) => void;
}) {
  const [collapsedTiers, setCollapsedTiers] = useState<Set<string>>(new Set());
  const [newTierName, setNewTierName] = useState("");

  const sizes = value || {};
  const tierNames = Object.keys(sizes);

  const toggleCollapse = (tier: string) => {
    setCollapsedTiers(prev => {
      const next = new Set(prev);
      next.has(tier) ? next.delete(tier) : next.add(tier);
      return next;
    });
  };

  const addTier = (tierName: string) => {
    const name = tierName.trim();
    if (!name || sizes[name]) return;
    const next = { ...sizes, [name]: {} };
    onChange(next);
    setNewTierName("");
  };

  const removeTier = (tier: string) => {
    const next = { ...sizes };
    delete next[tier];
    onChange(Object.keys(next).length > 0 ? next : undefined);
  };

  const addRatio = (tier: string, ratio: string) => {
    if (!ratio.trim()) return;
    const next = { ...sizes, [tier]: { ...sizes[tier], [ratio.trim()]: "" } };
    onChange(next);
  };

  const updateResolution = (tier: string, ratio: string, resolution: string) => {
    const next = { ...sizes, [tier]: { ...sizes[tier], [ratio]: resolution } };
    onChange(next);
  };

  const removeRatio = (tier: string, ratio: string) => {
    const tierData = { ...sizes[tier] };
    delete tierData[ratio];
    const next = { ...sizes, [tier]: tierData };
    onChange(next);
  };

  const fillCommonRatios = (tier: string) => {
    const tierData = { ...sizes[tier] };
    COMMON_ASPECT_RATIOS.forEach(r => {
      if (!(r in tierData)) tierData[r] = "";
    });
    onChange({ ...sizes, [tier]: tierData });
  };

  const availableTierChips = COMMON_TIERS.filter(t => !sizes[t]);

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <label className="text-[11px] text-muted-foreground">支持的尺寸 (supportedSizes)</label>
      </div>

      {tierNames.map(tier => {
        const isCollapsed = collapsedTiers.has(tier);
        const ratios = sizes[tier] || {};
        const ratioEntries = Object.entries(ratios);

        return (
          <div key={tier} className="rounded-lg border border-border/30 bg-background overflow-hidden">
            <div className="flex items-center justify-between px-3 py-1.5 bg-muted/30 border-b border-border/20">
              <button
                type="button"
                onClick={() => toggleCollapse(tier)}
                className="flex items-center gap-1.5 text-[11px] font-medium text-foreground/80 hover:text-foreground transition-colors"
              >
                <ChevronRight className={cn("h-3 w-3 transition-transform duration-200", !isCollapsed && "rotate-90")} />
                {tier}
                <span className="text-[10px] text-muted-foreground font-normal">({ratioEntries.length} 项)</span>
              </button>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => fillCommonRatios(tier)}
                  className="text-[9px] text-muted-foreground hover:text-primary transition-colors px-1"
                  title="填充常见比例"
                >
                  +常见比例
                </button>
                <button
                  type="button"
                  onClick={() => removeTier(tier)}
                  className="text-[9px] text-muted-foreground hover:text-destructive transition-colors"
                  title="删除档位"
                >
                  <Trash2 className="h-3 w-3" />
                </button>
              </div>
            </div>

            {!isCollapsed && (
              <div className="p-2 space-y-1">
                {ratioEntries.map(([ratio, resolution]) => (
                  <div key={ratio} className="flex items-center gap-1.5">
                    <span className="text-[10px] text-muted-foreground w-10 shrink-0 text-right font-mono">{ratio}</span>
                    <Input
                      placeholder="例如：1024x1024"
                      value={resolution}
                      onChange={e => updateResolution(tier, ratio, e.target.value)}
                      className="text-[10px] font-mono h-6 flex-1"
                    />
                    <button
                      type="button"
                      onClick={() => removeRatio(tier, ratio)}
                      className="text-[9px] text-muted-foreground hover:text-destructive transition-colors shrink-0"
                    >
                      ✕
                    </button>
                  </div>
                ))}

                {(() => {
                  const existingRatios = new Set(Object.keys(ratios));
                  const available = COMMON_ASPECT_RATIOS.filter(r => !existingRatios.has(r));
                  if (available.length === 0) return null;
                  return (
                    <div className="flex flex-wrap gap-1 pt-1 border-t border-border/10 mt-1">
                      {available.map(r => (
                        <button
                          key={r}
                          type="button"
                          onClick={() => addRatio(tier, r)}
                          className="text-[9px] px-1.5 py-0.5 rounded border border-dashed border-border/40 text-muted-foreground hover:border-primary/40 hover:text-primary transition-colors"
                        >
                          +{r}
                        </button>
                      ))}
                    </div>
                  );
                })()}
              </div>
            )}
          </div>
        );
      })}

      <div className="flex items-center gap-1.5">
        {availableTierChips.map(t => (
          <button
            key={t}
            type="button"
            onClick={() => addTier(t)}
            className="text-[10px] px-2 py-0.5 rounded-md border border-dashed border-border/40 text-muted-foreground hover:border-primary/40 hover:text-primary transition-colors"
          >
            +{t}
          </button>
        ))}
        <div className="flex items-center gap-1 ml-auto">
          <Input
            placeholder="自定义档位"
            value={newTierName}
            onChange={e => setNewTierName(e.target.value)}
            onKeyDown={e => e.key === "Enter" && addTier(newTierName)}
            className="text-[10px] h-5 w-20 font-mono"
          />
          <button
            type="button"
            onClick={() => addTier(newTierName)}
            disabled={!newTierName.trim()}
            className="text-[10px] px-1.5 py-0.5 rounded text-primary hover:bg-primary/10 transition-colors disabled:opacity-40"
          >
            添加
          </button>
        </div>
      </div>
    </div>
  );
}

// ---------- supportedAspectRatios 编辑器 ----------

function AspectRatiosEditor({
  value,
  onChange,
  label,
  hint,
  presetOptions,
}: {
  value: string[] | undefined;
  onChange: (v: string[] | undefined) => void;
  label?: string;
  hint?: string;
  presetOptions?: string[];
}) {
  const [customRatio, setCustomRatio] = useState("");
  const selected = new Set(value || []);
  const commonOptions = presetOptions || COMMON_ASPECT_RATIOS;

  const toggle = (ratio: string) => {
    const next = new Set(selected);
    next.has(ratio) ? next.delete(ratio) : next.add(ratio);
    const arr = Array.from(next);
    onChange(arr.length > 0 ? arr : undefined);
  };

  const addCustom = () => {
    const r = customRatio.trim();
    if (!r || selected.has(r)) return;
    onChange([...Array.from(selected), r]);
    setCustomRatio("");
  };

  return (
    <div className="space-y-2.5">
      <label className="text-[11px] text-muted-foreground">{label || "支持的比例 (supportedAspectRatios)"}</label>
      {hint && <p className="text-[10px] text-muted-foreground/70 -mt-1">{hint}</p>}
      <div className="flex flex-wrap gap-1.5">
        {commonOptions.map(ratio => (
          <button
            key={ratio}
            type="button"
            onClick={() => toggle(ratio)}
            className={cn(
              "inline-flex items-center gap-1 text-[10px] px-2.5 py-1 rounded-full border transition-all duration-150",
              selected.has(ratio)
                ? "border-primary/40 bg-primary/15 text-primary font-medium shadow-sm shadow-primary/5"
                : "border-dashed border-border/50 text-muted-foreground/60 hover:border-primary/30 hover:text-foreground"
            )}
          >
            {selected.has(ratio) && <Check className="h-2.5 w-2.5" />}
            {ratio}
          </button>
        ))}
        {Array.from(selected)
          .filter(r => !commonOptions.includes(r))
          .map(ratio => (
            <button
              key={ratio}
              type="button"
              onClick={() => toggle(ratio)}
              className="inline-flex items-center gap-1 text-[10px] px-2.5 py-1 rounded-full border border-primary/40 bg-primary/15 text-primary font-medium shadow-sm shadow-primary/5 transition-all duration-150"
            >
              <Check className="h-2.5 w-2.5" />
              {ratio}
              <span className="text-primary/50 ml-0.5">✕</span>
            </button>
          ))}
      </div>
      <div className="flex items-center gap-2 pt-1 border-t border-border/20">
        <Input
          placeholder="自定义值，如：16:9"
          value={customRatio}
          onChange={e => setCustomRatio(e.target.value)}
          onKeyDown={e => e.key === "Enter" && addCustom()}
          className="text-[10px] h-6 w-50 font-mono"
        />
        <button
          type="button"
          onClick={addCustom}
          disabled={!customRatio.trim()}
          className="text-[10px] px-2 py-0.5 rounded-md text-primary hover:bg-primary/10 transition-colors disabled:opacity-30"
        >
          + 添加
        </button>
      </div>
    </div>
  );
}

// ---------- ModelConfigForm 主组件 ----------

interface ConfigFieldDef {
  key: string;
  label: string;
  type: "number" | "range" | "supported-sizes" | "aspect-ratios";
  min?: number;
  max?: number;
  step?: number;
  placeholder?: string;
  hint?: string;
  presetOptions?: string[];
}

function getConfigFieldsByModelType(modelType: number): ConfigFieldDef[] {
  switch (modelType) {
    case 1:
      return [
        { key: "temperature", label: "Temperature", type: "range", min: 0, max: 2, step: 0.1, hint: "控制输出随机性，值越高越随机" },
        { key: "topP", label: "Top P", type: "range", min: 0, max: 1, step: 0.05, hint: "核心采样概率阈值" },
        { key: "maxTokens", label: "Max Tokens", type: "number", min: 1, max: 1000000, step: 1, placeholder: "例如：4096", hint: "单次请求最大输出 token 数" },
      ];
    case 2:
      return [
        { key: "defaultWidth", label: "默认宽度", type: "number", min: 256, max: 8192, step: 64, placeholder: "例如：1024" },
        { key: "defaultHeight", label: "默认高度", type: "number", min: 256, max: 8192, step: 64, placeholder: "例如：1024" },
        { key: "minPixels", label: "最小像素数", type: "number", min: 0, step: 1, placeholder: "例如：921600", hint: "生成图像的最小总像素数限制" },
        { key: "maxPixels", label: "最大像素数", type: "number", min: 0, step: 1, placeholder: "例如：16777216", hint: "生成图像的最大总像素数限制" },
        { key: "supportedSizes", label: "支持的尺寸", type: "supported-sizes" },
        { key: "supportedAspectRatios", label: "支持的比例", type: "aspect-ratios" },
      ];
    case 3:
      return [
        { key: "supportedResolutions", label: "支持的分辨率", type: "aspect-ratios", hint: "如 480p, 720p, 1080p", presetOptions: ["480p", "720p", "1080p", "2K", "4K"] },
        { key: "supportedAspectRatios", label: "支持的宽高比", type: "aspect-ratios", hint: "如 16:9, 9:16, 1:1" },
        { key: "minDuration", label: "最短时长（秒）", type: "number", min: 1, max: 60, step: 1, placeholder: "例如：4" },
        { key: "maxDuration", label: "最长时长（秒）", type: "number", min: 1, max: 60, step: 1, placeholder: "例如：15" },
        { key: "defaultDuration", label: "默认时长（秒）", type: "number", min: 1, max: 60, step: 1, placeholder: "例如：5" },
      ];
    default:
      return [];
  }
}

function ModelConfigForm({
  modelType,
  configJson,
  onChange,
}: {
  modelType: number;
  configJson: string | undefined;
  onChange: (json: string) => void;
}) {
  const [showRawJson, setShowRawJson] = useState(false);
  const [rawJsonDraft, setRawJsonDraft] = useState("");
  const [rawJsonError, setRawJsonError] = useState(false);

  const configObj = parseConfigJson(configJson);
  const fields = getConfigFieldsByModelType(modelType);

  const emitChange = (next: Record<string, unknown>) => {
    const cleaned = Object.fromEntries(
      Object.entries(next).filter(([, v]) => v !== "" && v !== undefined && v !== null)
    );
    onChange(Object.keys(cleaned).length > 0 ? JSON.stringify(cleaned) : "");
  };

  const updateSimpleField = (key: string, raw: string) => {
    const next = { ...configObj };
    if (raw === "" || raw === undefined) {
      delete next[key];
    } else {
      const num = parseFloat(raw);
      next[key] = isNaN(num) ? raw : num;
    }
    emitChange(next);
  };

  const updateComplexField = (key: string, value: unknown) => {
    const next = { ...configObj };
    if (value === undefined || value === null) {
      delete next[key];
    } else {
      next[key] = value;
    }
    emitChange(next);
  };

  const handleToggleRawJson = () => {
    if (!showRawJson) {
      setRawJsonDraft(configJson ? JSON.stringify(parseConfigJson(configJson), null, 2) : "");
      setRawJsonError(false);
    }
    setShowRawJson(!showRawJson);
  };

  const handleRawJsonApply = () => {
    if (!rawJsonDraft.trim()) {
      onChange("");
      setShowRawJson(false);
      return;
    }
    try {
      JSON.parse(rawJsonDraft);
      onChange(rawJsonDraft);
      setRawJsonError(false);
      setShowRawJson(false);
    } catch {
      setRawJsonError(true);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <Label className="text-xs text-muted-foreground">模型参数配置</Label>
        <button
          type="button"
          onClick={handleToggleRawJson}
          className="text-[10px] text-muted-foreground hover:text-foreground transition-colors px-1.5 py-0.5 rounded hover:bg-muted/50"
        >
          {showRawJson ? "切换表单" : "编辑 JSON"}
        </button>
      </div>

      {showRawJson ? (
        <div className="space-y-2">
          <textarea
            value={rawJsonDraft}
            onChange={e => {
              setRawJsonDraft(e.target.value);
              setRawJsonError(false);
            }}
            placeholder='{"temperature": 0.7, "maxTokens": 4096}'
            className={cn(
              "w-full min-h-[120px] rounded-lg border bg-background px-3 py-2 text-xs font-mono resize-y",
              "focus:outline-none focus:ring-2 focus:ring-ring/30",
              rawJsonError && "border-destructive focus:ring-destructive/30"
            )}
          />
          {rawJsonError && (
            <p className="text-[10px] text-destructive">JSON 格式错误，请检查语法</p>
          )}
          <div className="flex justify-end gap-1.5">
            <button
              type="button"
              onClick={() => setShowRawJson(false)}
              className="text-[10px] px-2 py-1 rounded text-muted-foreground hover:text-foreground hover:bg-muted/50 transition-colors"
            >
              取消
            </button>
            <button
              type="button"
              onClick={handleRawJsonApply}
              className="text-[10px] px-2 py-1 rounded bg-primary/10 text-primary hover:bg-primary/20 transition-colors"
            >
              应用
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {fields.length > 0 ? (
            <>
              {(() => {
                const simpleFields = fields.filter(f => f.type === "number" || f.type === "range");
                if (simpleFields.length === 0) return null;
                return (
                  <div className="rounded-lg border border-border/40 bg-muted/20 p-3 space-y-3">
                    {simpleFields.map(field => {
                      const value = configObj[field.key];
                      const numValue = typeof value === "number" ? value : (typeof value === "string" ? parseFloat(value) : undefined);

                      if (field.type === "range") {
                        return (
                          <div key={field.key} className="space-y-1.5">
                            <div className="flex items-center justify-between">
                              <label className="text-[11px] text-muted-foreground">{field.label}</label>
                              <span className="text-[11px] font-mono text-foreground/80 tabular-nums min-w-[3ch] text-right">
                                {numValue !== undefined && !isNaN(numValue) ? numValue : "—"}
                              </span>
                            </div>
                            <div className="flex items-center gap-2">
                              <input
                                type="range"
                                min={field.min ?? 0}
                                max={field.max ?? 1}
                                step={field.step ?? 0.1}
                                value={numValue !== undefined && !isNaN(numValue) ? numValue : field.min ?? 0}
                                onChange={e => updateSimpleField(field.key, e.target.value)}
                                className="flex-1 h-1.5 accent-primary cursor-pointer"
                              />
                              <button
                                type="button"
                                onClick={() => updateSimpleField(field.key, "")}
                                className="text-[9px] text-muted-foreground hover:text-destructive transition-colors shrink-0"
                                title="清除"
                              >
                                ✕
                              </button>
                            </div>
                            {field.hint && <p className="text-[10px] text-muted-foreground/70">{field.hint}</p>}
                          </div>
                        );
                      }

                      return (
                        <div key={field.key} className="space-y-1">
                          <label className="text-[11px] text-muted-foreground">{field.label}</label>
                          <Input
                            type="number"
                            min={field.min}
                            max={field.max}
                            step={field.step}
                            placeholder={field.placeholder}
                            value={numValue !== undefined && !isNaN(numValue) ? numValue : ""}
                            onChange={e => updateSimpleField(field.key, e.target.value)}
                            className="text-xs font-mono h-8"
                          />
                          {field.hint && <p className="text-[10px] text-muted-foreground/70">{field.hint}</p>}
                        </div>
                      );
                    })}
                  </div>
                );
              })()}

              {fields.some(f => f.type === "supported-sizes") && (
                <div className="rounded-lg border border-border/40 bg-muted/20 p-3">
                  <SupportedSizesEditor
                    value={configObj.supportedSizes as SizesMap | undefined}
                    onChange={v => updateComplexField("supportedSizes", v)}
                  />
                </div>
              )}

              {fields
                .filter(f => f.type === "aspect-ratios")
                .map(field => (
                  <div key={field.key} className="rounded-lg border border-border/40 bg-muted/20 p-3">
                    <AspectRatiosEditor
                      label={field.label}
                      hint={field.hint}
                      presetOptions={field.presetOptions}
                      value={configObj[field.key] as string[] | undefined}
                      onChange={v => updateComplexField(field.key, v)}
                    />
                  </div>
                ))}
            </>
          ) : (
            <p className="text-[10px] text-muted-foreground/60 italic">当前模型类型无预定义配置项，可点击「编辑 JSON」手动配置</p>
          )}
        </div>
      )}
    </div>
  );
}

// ============================================================
// AI 模型 Dialog
// ============================================================

interface AiModelDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editingModel: AiModel | null;
  apiConfigs: ApiConfig[];
  defaultApiConfigId?: number;
  onSaved: () => void;
}

function AiModelDialog({ open, onOpenChange, editingModel, apiConfigs, defaultApiConfigId, onSaved }: AiModelDialogProps) {
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<AiModelCreateReq & { id?: number; status?: number }>({
    name: "",
    code: "",
    modelType: 1,
  });
  const [presets, setPresets] = useState<ModelPreset[]>([]);
  const [selectedPresetCode, setSelectedPresetCode] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      if (editingModel) {
        setForm({
          id: editingModel.id,
          name: editingModel.name,
          code: editingModel.code,
          modelType: editingModel.modelType,
          description: editingModel.description || "",
          config: editingModel.config || "",
          defaultModel: editingModel.defaultModel,
          apiConfigId: editingModel.apiConfigId ?? undefined,
          status: editingModel.status,
        });
        setSelectedPresetCode(null);
      } else {
        setForm({ name: "", code: "", modelType: 1, defaultModel: false, apiConfigId: defaultApiConfigId });
        setSelectedPresetCode(null);
        const currentPlatform = apiConfigs.find(c => c.id === defaultApiConfigId)?.platform;
        aiModelApi.presets().then(all => {
          setPresets(currentPlatform ? all.filter(p => p.platform === currentPlatform) : all);
        }).catch(console.error);
      }
    }
  }, [open, editingModel, defaultApiConfigId, apiConfigs]);

  const updateField = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => {
    setForm(prev => ({ ...prev, [key]: value }));
  };

  const handleSave = async () => {
    if (!form.name.trim() || !form.code.trim()) return;
    setSaving(true);
    try {
      if (editingModel) {
        const updateReq: AiModelUpdateReq = {
          id: editingModel.id,
          name: form.name,
          code: form.code,
          modelType: form.modelType,
          description: form.description,
          config: form.config,
          defaultModel: form.defaultModel,
          apiConfigId: form.apiConfigId,
          status: form.status,
        };
        await aiModelApi.update(updateReq);
      } else {
        await aiModelApi.create(form);
      }
      onSaved();
      onOpenChange(false);
    } catch (err) {
      console.error("保存 AI 模型失败:", err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg flex flex-col max-h-[85vh]">
        <DialogHeader className="shrink-0">
          <DialogTitle>{editingModel ? "编辑 AI 模型" : "新建 AI 模型"}</DialogTitle>
          <DialogDescription>
            配置可用的 AI 模型参数
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 overflow-y-auto min-h-0 px-2 -mx-2">
          {/* 预设快速选择 */}
          {!editingModel && presets.length > 0 && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Sparkles className="h-3 w-3" />
                从预设导入
              </Label>
              <div className="flex flex-wrap gap-1.5">
                {presets.map(preset => (
                  <button
                    key={preset.code}
                    type="button"
                    onClick={() => {
                      setSelectedPresetCode(preset.code);
                      setForm(prev => ({
                        ...prev,
                        name: preset.name,
                        code: preset.code,
                        modelType: preset.modelType,
                        description: preset.description,
                        config: JSON.stringify(preset.config),
                      }));
                    }}
                    className={cn(
                      "inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs transition-all duration-200",
                      "border",
                      selectedPresetCode === preset.code
                        ? "border-primary/50 bg-primary/10 text-primary font-medium"
                        : "border-border/30 text-muted-foreground hover:border-primary/30 hover:text-foreground hover:bg-white/5"
                    )}
                  >
                    {selectedPresetCode === preset.code && <Check className="h-3 w-3" />}
                    {preset.name}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* 模型名称 */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">模型名称 <span className="text-destructive">*</span></Label>
            <Input
              placeholder="例如：DeepSeek Chat / GPT-4o"
              value={form.name}
              onChange={e => updateField("name", e.target.value)}
              className="text-sm"
            />
          </div>

          {/* 模型标识 */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">模型标识 (code) <span className="text-destructive">*</span></Label>
            <Input
              placeholder="例如：deepseek-chat / gpt-4o"
              value={form.code}
              onChange={e => updateField("code", e.target.value)}
              className="text-sm font-mono"
            />
            <p className="text-[10px] text-muted-foreground">对应 API 中实际使用的 model 名称</p>
          </div>

          {/* 模型类型 */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">模型类型</Label>
            <Select
              value={form.modelType}
              onValueChange={v => updateField("modelType", v as number)}
              items={MODEL_TYPE_OPTIONS.map(o => ({ value: o.value, label: o.label }))}
            >
              <SelectTrigger className="w-full text-sm">
                <SelectValue placeholder="选择类型" />
              </SelectTrigger>
              <SelectContent className="text-sm">
                <SelectGroup>
                  {MODEL_TYPE_OPTIONS.map(opt => (
                    <SelectItem key={opt.value} value={opt.value} className="text-sm">
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>

          {/* 关联 API 配置 */}
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">关联 API 配置 <span className="text-destructive">*</span></Label>
            <Select
              value={form.apiConfigId}
              onValueChange={v => updateField("apiConfigId", v as number)}
              items={apiConfigs.map(c => ({ value: c.id, label: `${c.name}${c.platform ? ` (${PLATFORM_LABELS[c.platform] || c.platform})` : ""}` }))}
            >
              <SelectTrigger className="w-full text-sm">
                <SelectValue placeholder="选择 API 配置" />
              </SelectTrigger>
              <SelectContent className="text-sm">
                <SelectGroup>
                  {apiConfigs.map(c => (
                    <SelectItem key={c.id} value={c.id} className="text-sm">
                      {c.name}
                      {c.platform && <span className="text-muted-foreground ml-1">({PLATFORM_LABELS[c.platform] || c.platform})</span>}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
            <p className="text-[10px] text-muted-foreground">模型将使用该 API 配置中的密钥进行调用</p>
          </div>

          {/* 默认模型 */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => updateField("defaultModel", !form.defaultModel)}
              className={cn(
                "relative w-9 h-5 rounded-full transition-colors duration-200",
                form.defaultModel ? "bg-primary" : "bg-muted-foreground/30"
              )}
            >
              <span
                className={cn(
                  "absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform duration-200",
                  form.defaultModel && "translate-x-4"
                )}
              />
            </button>
            <Label className="text-xs text-muted-foreground cursor-pointer" onClick={() => updateField("defaultModel", !form.defaultModel)}>
              设为默认模型
            </Label>
          </div>

          {/* 模型配置 */}
          <ModelConfigForm
            modelType={form.modelType}
            configJson={form.config}
            onChange={json => updateField("config", json)}
          />
        </div>

        <DialogFooter className="shrink-0">
          <DialogClose render={<Button variant="outline" size="sm" />}>
            取消
          </DialogClose>
          <Button size="sm" onClick={handleSave} disabled={saving || !form.name.trim() || !form.code.trim()}>
            {saving && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />}
            {editingModel ? "保存" : "创建"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ============================================================
// 主页面
// ============================================================

export default function AiModelsPage() {
  // AI 模型列表
  const [models, setModels] = useState<AiModel[]>([]);
  const [modelsLoading, setModelsLoading] = useState(true);

  // API 配置列表
  const [configs, setConfigs] = useState<ApiConfig[]>([]);
  const [configsLoading, setConfigsLoading] = useState(true);

  // Dialog 状态
  const [configDialogOpen, setConfigDialogOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState<ApiConfig | null>(null);
  const [modelDialogOpen, setModelDialogOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<AiModel | null>(null);
  const [modelDialogApiConfigId, setModelDialogApiConfigId] = useState<number | undefined>(undefined);

  useEffect(() => {
    loadModels();
    loadConfigs();
  }, []);

  const loadModels = useCallback(async () => {
    try {
      setModelsLoading(true);
      const data = await aiModelApi.list();
      setModels(data);
    } catch (err) {
      console.error("加载 AI 模型列表失败:", err);
    } finally {
      setModelsLoading(false);
    }
  }, []);

  const loadConfigs = useCallback(async () => {
    try {
      setConfigsLoading(true);
      const data = await apiConfigApi.list();
      setConfigs(data);
    } catch (err) {
      console.error("加载 API 配置列表失败:", err);
    } finally {
      setConfigsLoading(false);
    }
  }, []);

  const handleDeleteModel = async (id: number) => {
    if (!confirm("确定要删除该 AI 模型吗？")) return;
    try {
      await aiModelApi.delete(id);
      await loadModels();
    } catch (err) {
      console.error("删除模型失败:", err);
    }
  };

  const handleDeleteConfig = async (id: number) => {
    if (!confirm("确定要删除该 API 配置吗？")) return;
    try {
      await apiConfigApi.delete(id);
      await loadConfigs();
    } catch (err) {
      console.error("删除配置失败:", err);
    }
  };

  return (
    <motion.div
      className="max-w-[1200px]"
      variants={containerVariants}
      initial="hidden"
      animate="visible"
    >
      {/* 页面标题 */}
      <motion.div variants={itemVariants} className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight">AI 服务管理</h1>
        <p className="text-muted-foreground mt-1">
          管理 API 配置和 AI 模型
        </p>
      </motion.div>

      {/* ========== AI 服务管理（统一卡片式） ========== */}
      <motion.div variants={itemVariants} className="mb-8">
        <div className="flex items-center justify-between mb-3 px-1">
          <h3 className="text-sm font-medium text-muted-foreground uppercase tracking-wider">
            API 配置与模型
          </h3>
          <button
            onClick={() => { setEditingConfig(null); setConfigDialogOpen(true); }}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium",
              "border border-dashed border-border/40 hover:border-primary/50",
              "text-muted-foreground hover:text-primary",
              "transition-all duration-200"
            )}
          >
            <Plus className="h-3.5 w-3.5" />
            添加 API 配置
          </button>
        </div>

        <div className="space-y-4">
          {configsLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ) : configs.length === 0 ? (
            <div className={cn(
              "rounded-xl border border-dashed border-border/30 py-10 text-center",
              "bg-card/30"
            )}>
              <Settings2 className="h-8 w-8 text-muted-foreground/20 mx-auto mb-2" />
              <p className="text-sm text-muted-foreground">还没有 API 配置</p>
              <p className="text-xs text-muted-foreground/60 mt-1">点击上方「添加 API 配置」开始</p>
            </div>
          ) : (
            configs.map((config) => {
              const pColor = platformIconColors[config.platform || ""] || { color: "text-green-400", bg: "bg-green-500/10" };
              const configModels = models.filter(m => m.apiConfigId === config.id);

              // 按类型分组
              const modelTypeGroups = [
                { type: 2, label: "图像生成" },
                { type: 3, label: "视频生成" },
                { type: 1, label: "对话" },
              ].map(g => ({
                ...g,
                models: configModels.filter(m => m.modelType === g.type),
              })).filter(g => g.models.length > 0);

              return (
                <div
                  key={config.id}
                  className={cn(
                    "rounded-xl border overflow-hidden",
                    "bg-card/50 backdrop-blur-sm border-border/50"
                  )}
                >
                  {/* ── API 配置头部 ── */}
                  <div className="flex items-center gap-3 px-4 py-3 group border-b border-border/40">
                    <div className={cn("h-9 w-9 rounded-lg flex items-center justify-center shrink-0", pColor.bg)}>
                      <Settings2 className={cn("h-4.5 w-4.5", pColor.color)} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium">{config.name}</p>
                        {config.platform && (
                          <span className="px-1.5 py-0.5 rounded bg-muted/50 text-[10px] text-muted-foreground">
                            {PLATFORM_LABELS[config.platform] || config.platform}
                          </span>
                        )}
                        <div className={cn(
                          "w-1.5 h-1.5 rounded-full shrink-0",
                          config.status === 1 ? "bg-green-400" : "bg-muted-foreground/30"
                        )} />
                      </div>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground mt-0.5">
                        {config.apiKey && (
                          <span className="font-mono text-[10px]">{maskSecret(config.apiKey)}</span>
                        )}
                        <span>{configModels.length} 个模型</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => { setEditingConfig(config); setConfigDialogOpen(true); }}
                        className="p-1.5 rounded-md text-muted-foreground hover:text-primary hover:bg-primary/10 transition-colors"
                      >
                        <Edit2 className="h-3.5 w-3.5" />
                      </button>
                      <button
                        onClick={() => handleDeleteConfig(config.id)}
                        className="p-1.5 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* ── 模型列表（按类型分组） ── */}
                  <div className="px-4 py-2">
                    {modelTypeGroups.length === 0 ? (
                      <p className="text-xs text-muted-foreground/50 text-center py-3">
                        暂无模型
                      </p>
                    ) : (
                      modelTypeGroups.map((group, gi) => {
                        const defaultModel = group.models.find(m => m.defaultModel);

                        return (
                          <div key={group.type} className={cn(gi > 0 && "mt-2 pt-2 border-t border-border/30")}>
                            {/* 类型分组标题 */}
                            <div className="flex items-center gap-2 px-1 py-1">
                              <span className="text-[11px] font-medium text-muted-foreground shrink-0">
                                {group.label}
                              </span>
                              <div className="flex-1 h-px bg-border/15" />
                              {defaultModel ? (
                                <span className="inline-flex items-center gap-1 text-[10px] text-amber-500/80 font-medium shrink-0">
                                  <Star className="h-2.5 w-2.5" />
                                  默认: {defaultModel.name}
                                </span>
                              ) : (
                                <span className="text-[10px] text-muted-foreground/30 shrink-0">未设默认</span>
                              )}
                            </div>

                            {/* 模型行 */}
                            {group.models.map((model) => (
                              <div
                                key={model.id}
                                className="flex items-center gap-3 px-3 py-2.5 rounded-lg group/model hover:bg-white/5 transition-colors"
                              >
                                <div className="h-7 w-7 rounded-md bg-primary/8 flex items-center justify-center shrink-0">
                                  <Bot className="h-3.5 w-3.5 text-primary/60" />
                                </div>
                                <div className="flex-1 min-w-0">
                                  <div className="flex items-center gap-2">
                                    <p className="text-sm font-medium">{model.name}</p>
                                    {model.defaultModel && (
                                      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-amber-500/10 text-[10px] text-amber-500 font-medium">
                                        <Star className="h-2.5 w-2.5" />
                                        默认
                                      </span>
                                    )}
                                    <div className={cn(
                                      "w-1.5 h-1.5 rounded-full shrink-0",
                                      model.status === 1 ? "bg-green-400" : "bg-muted-foreground/30"
                                    )} />
                                  </div>
                                  <div className="flex items-center gap-1.5 text-[10px] text-muted-foreground mt-0.5">
                                    <span className="font-mono px-1 py-0.5 rounded bg-muted/40">{model.code}</span>
                                  </div>
                                </div>

                                {/* 操作按钮 */}
                                <div className="flex items-center gap-1 shrink-0 opacity-0 group-hover/model:opacity-100 transition-opacity">
                                  {!model.defaultModel && (
                                    <button
                                      onClick={async () => {
                                        try {
                                          const sameTypeModels = models.filter(m => m.modelType === model.modelType && m.defaultModel);
                                          for (const dm of sameTypeModels) {
                                            await aiModelApi.update({ id: dm.id, defaultModel: false });
                                          }
                                          await aiModelApi.update({ id: model.id, defaultModel: true });
                                          await loadModels();
                                        } catch (err) {
                                          console.error("设置默认模型失败:", err);
                                        }
                                      }}
                                      className={cn(
                                        "flex items-center gap-1 px-2 py-1 rounded-md text-[10px] font-medium transition-all",
                                        "border border-amber-500/30 text-amber-500",
                                        "hover:bg-amber-500/10 hover:border-amber-500/50"
                                      )}
                                    >
                                      <Star className="h-3 w-3" />
                                      设为默认
                                    </button>
                                  )}
                                  <button
                                    onClick={() => { setEditingModel(model); setModelDialogApiConfigId(undefined); setModelDialogOpen(true); }}
                                    className="p-1 rounded-md text-muted-foreground/50 hover:text-primary hover:bg-primary/10 transition-colors"
                                  >
                                    <Edit2 className="h-3 w-3" />
                                  </button>
                                  <button
                                    onClick={() => handleDeleteModel(model.id)}
                                    className="p-1 rounded-md text-muted-foreground/50 hover:text-destructive hover:bg-destructive/10 transition-colors"
                                  >
                                    <Trash2 className="h-3 w-3" />
                                  </button>
                                </div>
                              </div>
                            ))}
                          </div>
                        );
                      })
                    )}

                    {/* 添加模型按钮 */}
                    <button
                      onClick={() => {
                        setEditingModel(null);
                        setModelDialogApiConfigId(config.id);
                        setModelDialogOpen(true);
                      }}
                      className={cn(
                        "flex items-center gap-2 w-full px-3 py-2 mt-1 rounded-lg",
                        "border border-dashed border-border/40 hover:border-primary/40",
                        "text-xs text-muted-foreground/60 hover:text-primary",
                        "transition-all duration-200"
                      )}
                    >
                      <Plus className="h-3.5 w-3.5" />
                      添加模型
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </motion.div>

      {/* Dialogs */}
      <ApiConfigDialog
        open={configDialogOpen}
        onOpenChange={setConfigDialogOpen}
        editingConfig={editingConfig}
        onSaved={() => { loadConfigs(); loadModels(); }}
      />
      <AiModelDialog
        open={modelDialogOpen}
        onOpenChange={setModelDialogOpen}
        editingModel={editingModel}
        apiConfigs={configs}
        defaultApiConfigId={modelDialogApiConfigId}
        onSaved={() => { loadModels(); }}
      />
    </motion.div>
  );
}
