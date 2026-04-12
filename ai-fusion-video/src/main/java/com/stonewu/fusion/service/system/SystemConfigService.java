package com.stonewu.fusion.service.system;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stonewu.fusion.entity.system.SystemConfig;
import com.stonewu.fusion.mapper.system.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    /**
     * 获取配置值
     */
    @Cacheable(value = "systemConfig", key = "#key", unless = "#result == null")
    public String getValue(String key) {
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, key)
                        .last("LIMIT 1"));
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 设置配置值（不存在则创建）
     */
    @CacheEvict(value = "systemConfig", key = "#key")
    public void setValue(String key, String value) {
        SystemConfig existing = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, key)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setConfigValue(value);
            systemConfigMapper.updateById(existing);
        } else {
            SystemConfig config = SystemConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .build();
            systemConfigMapper.insert(config);
        }
    }

    /**
     * 获取所有配置
     */
    public List<SystemConfig> getAll() {
        return systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .orderByAsc(SystemConfig::getConfigKey));
    }

    /**
     * 获取站点访问域名
     */
    public String getSiteBaseUrl() {
        String url = getValue("site_base_url");
        // 去掉末尾斜杠
        if (StrUtil.isNotBlank(url) && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * 将相对路径解析为完整的公网可访问 URL
     * <p>
     * 1. 已是完整 URL (http/https) → 直接返回
     * 2. 有 site_base_url → 拼接
     * 3. 都没有 → 返回 null
     */
    public String resolvePublicUrl(String relativePath) {
        if (StrUtil.isBlank(relativePath)) {
            return null;
        }
        // 已经是完整 URL（如 OSS 直链）
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        // 拼接站点域名
        String siteBaseUrl = getSiteBaseUrl();
        if (StrUtil.isNotBlank(siteBaseUrl)) {
            return siteBaseUrl + relativePath;
        }
        return null;
    }
}
