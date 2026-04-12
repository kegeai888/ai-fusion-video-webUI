"use client";

import type { LucideIcon } from "lucide-react";
import {
  User,
  Mountain,
  Box,
  Car,
  Building2,
  Shirt,
  Sparkles,
  Images,
} from "lucide-react";
import { cn } from "@/lib/utils";

/** 类型→图标 + 渐变色配置 */
const typeConfig: Record<string, {
  icon: LucideIcon;
  gradient: string;
  glowColor: string;
  iconColor: string;
}> = {
  character: {
    icon: User,
    gradient: "from-blue-500/8 via-cyan-400/12 to-blue-600/8",
    glowColor: "bg-blue-400/20",
    iconColor: "text-blue-400/30",
  },
  scene: {
    icon: Mountain,
    gradient: "from-green-500/8 via-emerald-400/12 to-green-600/8",
    glowColor: "bg-green-400/20",
    iconColor: "text-green-400/30",
  },
  prop: {
    icon: Box,
    gradient: "from-amber-500/8 via-yellow-400/12 to-amber-600/8",
    glowColor: "bg-amber-400/20",
    iconColor: "text-amber-400/30",
  },
  vehicle: {
    icon: Car,
    gradient: "from-cyan-500/8 via-sky-400/12 to-cyan-600/8",
    glowColor: "bg-cyan-400/20",
    iconColor: "text-cyan-400/30",
  },
  building: {
    icon: Building2,
    gradient: "from-purple-500/8 via-violet-400/12 to-purple-600/8",
    glowColor: "bg-purple-400/20",
    iconColor: "text-purple-400/30",
  },
  costume: {
    icon: Shirt,
    gradient: "from-pink-500/8 via-rose-400/12 to-pink-600/8",
    glowColor: "bg-pink-400/20",
    iconColor: "text-pink-400/30",
  },
  effect: {
    icon: Sparkles,
    gradient: "from-orange-500/8 via-amber-400/12 to-orange-600/8",
    glowColor: "bg-orange-400/20",
    iconColor: "text-orange-400/30",
  },
};

const defaultConfig: { icon: LucideIcon; gradient: string; glowColor: string; iconColor: string } = {
  icon: Images,
  gradient: "from-muted/10 via-muted/15 to-muted/10",
  glowColor: "bg-muted/20",
  iconColor: "text-muted-foreground/20",
};

interface Props {
  type: string;
  className?: string;
  /** 图标大小 class，默认 h-8 w-8 */
  iconSize?: string;
}

/**
 * 资产类型占位图：不同类型显示不同图标 + 颜色流光动画
 */
export default function AssetTypePlaceholder({ type, className, iconSize = "h-8 w-8" }: Props) {
  const config = typeConfig[type] || defaultConfig;
  const Icon = config.icon;

  return (
    <div className={cn(
      "relative flex flex-col items-center justify-center overflow-hidden",
      `bg-linear-to-br ${config.gradient}`,
      className
    )}>
      {/* 流光效果 */}
      <div
        className={cn(
          "absolute inset-0 opacity-60",
          "before:absolute before:inset-0",
          "before:bg-linear-to-r before:from-transparent before:via-white/4 before:to-transparent",
          "before:animate-[shimmer_3s_ease-in-out_infinite]",
        )}
      />
      {/* 光晕 */}
      <div className={cn(
        "absolute rounded-full blur-2xl opacity-40 animate-pulse",
        config.glowColor,
        "w-1/2 h-1/2"
      )} />
      {/* 图标 */}
      <Icon className={cn(iconSize, config.iconColor, "relative z-10")} />
    </div>
  );
}
