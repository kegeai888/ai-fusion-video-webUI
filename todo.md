# 融光 AI 视频创作平台 — 二次开发记录 & 经验总结

> 记录时间：2026-04-26

---

## 一、已完成的修复与优化

### 1. 外网登录 Network Error 修复

**问题**：浏览器通过外网 IP 访问前端 7860 端口，登录时报 `Network Error`。

**根因**：`lib/api/client.ts` 中 `API_BASE_URL` 默认值为 `http://localhost:18080/api`，打包进浏览器 JS 后，外网用户的浏览器无法访问服务器本地的 18080 端口。

**修复方案**：
- 将 `NEXT_PUBLIC_API_BASE_URL` 改为 `/api`（同源相对路径）
- 在 `next.config.ts` 中添加 Next.js rewrite 规则，将 `/api/:path*` 代理转发到 `http://localhost:18080/api/:path*`
- 浏览器只需访问前端 7860 端口，不再直接访问后端 18080

**关键文件**：
- `ai-fusion-video-web/lib/api/client.ts` — `API_BASE_URL` 默认值改为 `"/api"`
- `ai-fusion-video-web/next.config.ts` — 添加 `rewrites()` 代理规则
- `ai-fusion-video-web/.env.local` — `NEXT_PUBLIC_API_BASE_URL=/api`，`BACKEND_API_BASE_URL=http://localhost:18080/api`

---

### 2. 后端启动失败修复

**问题**：`start_app.sh` 启动后端，健康检查返回 `000`，后端无法访问。

**根因**：MySQL 和 Redis 服务未启动，Flyway 无法获取数据库连接，Spring Boot 启动失败。

**修复方案**：
```bash
sudo service mysql start
sudo service redis-server start
```

**经验**：每次重启服务器后需先确认 MySQL/Redis 已启动，再运行 `start_app.sh`。

---

### 3. 前端端口冲突修复

**问题**：重启前端时报 `EADDRINUSE :::7860`，前端无法启动。

**根因**：旧的 `next-server` 进程未被 `pkill -f "next dev"` 杀掉（进程名为 `next-server` 而非 `next dev`）。

**修复方案**：在 `start_app.sh` 中增加：
```bash
pkill -9 -f "next-server" 2>/dev/null
```
同时按进程参数精确匹配 `ai-fusion-video-web`，避免误杀 JupyterLab。

---

### 4. 登录测试脚本端点修复

**问题**：`test_login.sh` 使用 `/auth/me` 端点，返回 null，测试失败。

**根因**：后端实际端点为 `/auth/user-info`，不存在 `/auth/me`。

**修复方案**：将脚本中所有 `/auth/me` 改为 `/auth/user-info`。

---

### 5. 启动脚本健康检查路径修复

**问题**：`start_app.sh` 健康检查访问 `/swagger-ui.html`，返回 `000`。

**根因**：后端 context-path 为 `/api`，正确路径应为 `/api/swagger-ui.html`。

**修复方案**：将检查 URL 改为 `http://localhost:18080/api/swagger-ui.html`。

---

## 二、新增工具脚本

| 脚本 | 用途 |
|------|------|
| `start_app.sh` | 一键启动前后端，自动清理端口冲突，输出日志路径 |
| `test_login.sh` | 测试后端连通性、认证拦截、登录、Token 有效性 |

---

## 三、配置变更汇总

| 文件 | 变更内容 |
|------|---------|
| `ai-fusion-video-web/next.config.ts` | 新增 `rewrites()` 同源代理 `/api` → 后端 |
| `ai-fusion-video-web/lib/api/client.ts` | `API_BASE_URL` 默认值改为 `"/api"` |
| `ai-fusion-video-web/.env.local` | `NEXT_PUBLIC_API_BASE_URL=/api`，`BACKEND_API_BASE_URL=http://localhost:18080/api` |
| `ai-fusion-video/src/main/resources/application-local.yaml` | MySQL/Redis 端口调整为标准端口 3306/6379 |

---

## 四、待办 & 后续优化建议

- [ ] **系统服务自启**：将 MySQL、Redis 配置为开机自启，避免每次重启服务器后手动启动
- [ ] **生产部署**：使用 Docker Compose 或 Nginx 反向代理，统一对外暴露 80/443 端口
- [ ] **HTTPS**：为外网访问配置 SSL 证书
- [ ] **日志轮转**：`backend.log` / `frontend.log` 无限增长，建议配置 logrotate
- [ ] **数据分析页**：前端 `/dashboard/analytics` 当前显示"功能开发中"，待实现
- [ ] **AI 模型配置**：需在系统设置中配置 API Key 才能使用 AI 生成功能
- [ ] **存储配置**：默认使用本地存储 `./data/media`，生产环境建议配置 S3/OSS

---

## 五、关键经验

1. **同源代理是外网部署的标准做法**：前端通过 Next.js rewrite 代理后端，浏览器只需访问一个端口，避免跨域和端口暴露问题。
2. **先验证依赖服务**：启动 Java 应用前，必须确认 MySQL 和 Redis 已运行。
3. **进程清理要精确**：`pkill` 按进程名匹配可能不够，需结合进程参数匹配，同时注意保护 JupyterLab 等共存进程。
4. **用实际代码验证端点**：测试脚本的 API 路径必须与 Controller 注解一致，不能凭猜测。
