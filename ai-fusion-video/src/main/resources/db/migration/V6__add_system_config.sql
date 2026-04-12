-- ==========================================================
-- V6: 新增系统配置表
-- ==========================================================

CREATE TABLE afv_system_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key      VARCHAR(128)    NOT NULL COMMENT '配置键',
    config_value    TEXT            NULL COMMENT '配置值',
    remark          VARCHAR(256)    NULL COMMENT '备注',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) COMMENT '系统配置表';
