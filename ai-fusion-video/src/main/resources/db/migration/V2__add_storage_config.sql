-- ==========================================================
-- V2: 添加存储配置表
-- ==========================================================

CREATE TABLE afv_storage_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(128)    NOT NULL COMMENT '配置名称',
    type            VARCHAR(32)     NOT NULL COMMENT '存储类型：local / aliyun_oss / tencent_cos / s3',
    endpoint        VARCHAR(512)    NULL COMMENT 'OSS 端点地址',
    bucket_name     VARCHAR(128)    NULL COMMENT 'OSS 存储桶名称',
    access_key      VARCHAR(256)    NULL COMMENT 'OSS Access Key',
    secret_key      VARCHAR(256)    NULL COMMENT 'OSS Secret Key',
    region          VARCHAR(64)     NULL COMMENT '区域',
    base_path       VARCHAR(256)    NULL COMMENT '存储根路径（本地为磁盘路径，OSS 为 key 前缀）',
    custom_domain   VARCHAR(512)    NULL COMMENT '自定义域名（CDN 域名等）',
    is_default      TINYINT         NOT NULL DEFAULT 0 COMMENT '是否为默认存储配置',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    remark          VARCHAR(1024)   NULL COMMENT '备注',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '存储配置表';
