---
trigger: always_on
description: Java 代码规范
---

# Java 代码规范

## Import 规范

- **禁止在代码中直接使用全限定类名**，必须先在文件头部 import
- 例如禁止 `new com.stonewu.fusion.common.BusinessException("xxx")`
- 应该先 `import com.stonewu.fusion.common.BusinessException;` 然后使用 `new BusinessException("xxx")`

## 缓存规范

- 所有 Service 使用 `@Cacheable` / `@CacheEvict` 管理缓存
- 在 `create`、`update`、`delete` 操作时清除相关缓存

## 安全规范

- 所有需要当前用户信息的地方使用 `SecurityUtils.requireCurrentUserId()`
- 禁止硬编码 userId
