import { http, API_BASE_URL } from "./client";
import axios from "axios";

// ========== 类型定义 ==========

export interface ArtStylePreset {
  key: string;
  name: string;
  description: string;
  imagePrompt: string;
  referenceImagePath: string;
  /** 上传到 OSS 后的公网 URL（全局） */
  referenceImagePublicUrl: string | null;
}

// ========== API ==========

export const artStyleApi = {
  /** 获取预设画风列表 */
  getPresets: () =>
    http.get<never, ArtStylePreset[]>("/api/project/presets/art-styles"),
};

/**
 * 上传文件（图片）
 * @returns 上传后的 URL
 */
export async function uploadFile(
  file: File,
  subDir: string = "art-styles"
): Promise<string> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("subDir", subDir);

  // 直接用 axios 避免 http 实例的拦截器处理 FormData
  const token = (() => {
    if (typeof window === "undefined") return null;
    try {
      const stored = localStorage.getItem("auth-storage");
      if (stored) return JSON.parse(stored)?.state?.token;
    } catch {
      // ignore
    }
    return null;
  })();

  const resp = await axios.post(`${API_BASE_URL}/api/storage/upload`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  const result = resp.data;
  if (result.code !== 0) {
    throw new Error(result.msg || "上传失败");
  }
  return result.data;
}
