"use client";

import {
  User,
  Bot,
  HardDrive,
  Globe,
  ChevronRight,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/lib/store/auth-store";
import { containerVariants, itemVariants } from "./_shared";

// 设置分组
const settingGroups = [
  {
    title: "个人",
    items: [
      {
        icon: User,
        label: "个人设置",
        description: "管理你的个人信息、密码和账户安全",
        iconColor: "text-blue-400",
        iconBg: "bg-blue-500/10",
        href: "/settings/profile",
      },
    ],
  },
  {
    title: "系统",
    items: [
      {
        icon: Globe,
        label: "通用设置",
        description: "配置项目访问域名等全局参数",
        iconColor: "text-emerald-400",
        iconBg: "bg-emerald-500/10",
        href: "/settings/general",
      },
      {
        icon: Bot,
        label: "AI 模型",
        description: "管理 API 接入配置和 AI 模型设置",
        iconColor: "text-purple-400",
        iconBg: "bg-purple-500/10",
        href: "/settings/ai-models",
      },
      {
        icon: HardDrive,
        label: "存储配置",
        description: "配置文件存储后端（本地磁盘 / S3）",
        iconColor: "text-orange-400",
        iconBg: "bg-orange-500/10",
        href: "/settings/storage",
      },
    ],
  },
];

export default function SettingsPage() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);

  // 用户名首字母（用于头像占位）
  const avatarInitial = (user?.nickname || user?.username || "U")
    .charAt(0)
    .toUpperCase();

  return (
    <motion.div
      className="max-w-[1200px]"
      variants={containerVariants}
      initial="hidden"
      animate="visible"
    >
      {/* 页面标题 */}
      <motion.div variants={itemVariants} className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight">系统设置</h1>
        <p className="text-muted-foreground mt-1">
          管理你的账户信息、应用偏好和系统配置
        </p>
      </motion.div>

      {/* 用户信息卡片 */}
      <motion.div
        variants={itemVariants}
        className={cn(
          "rounded-xl border border-border/30 p-6 mb-8",
          "bg-card/50 backdrop-blur-sm"
        )}
      >
        <div className="flex items-center gap-4">
          <div className="h-16 w-16 rounded-2xl bg-linear-to-br from-blue-500 via-purple-500 to-pink-500 flex items-center justify-center text-white text-xl font-bold shadow-lg">
            {avatarInitial}
          </div>
          <div className="flex-1">
            <h2 className="text-lg font-semibold">
              {user?.nickname || user?.username || "用户"}
            </h2>
            <p className="text-sm text-muted-foreground">
              {user?.email || user?.username || "未设置邮箱"}
            </p>
          </div>
          <button
            onClick={() => router.push("/settings/profile")}
            className={cn(
              "px-4 py-2 rounded-xl text-sm font-medium",
              "border border-border/30 bg-white/5",
              "text-muted-foreground hover:text-foreground hover:bg-white/10",
              "transition-all duration-200"
            )}
          >
            编辑资料
          </button>
        </div>
      </motion.div>

      {/* 设置分组 */}
      {settingGroups.map((group) => (
        <motion.div key={group.title} variants={itemVariants} className="mb-8">
          <h3 className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3 px-1">
            {group.title}
          </h3>
          <div
            className={cn(
              "rounded-xl border border-border/30 overflow-hidden",
              "bg-card/50 backdrop-blur-sm"
            )}
          >
            {group.items.map((item, index) => {
              const Icon = item.icon;
              return (
                <button
                  key={item.label}
                  onClick={() => router.push(item.href)}
                  className={cn(
                    "flex items-center gap-4 w-full px-5 py-4 text-left",
                    "hover:bg-white/5 transition-colors",
                    index !== group.items.length - 1 &&
                      "border-b border-border/20"
                  )}
                >
                  <div
                    className={cn(
                      "h-10 w-10 rounded-xl flex items-center justify-center shrink-0",
                      item.iconBg
                    )}
                  >
                    <Icon className={cn("h-5 w-5", item.iconColor)} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium">{item.label}</p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {item.description}
                    </p>
                  </div>
                  <ChevronRight className="h-4 w-4 text-muted-foreground/50 shrink-0" />
                </button>
              );
            })}
          </div>
        </motion.div>
      ))}
    </motion.div>
  );
}
