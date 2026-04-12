-- ==========================================================
-- V5: 项目表新增画风字段
-- ==========================================================

ALTER TABLE afv_project
    ADD COLUMN art_style              VARCHAR(64)   NULL COMMENT '画风key（预设key或custom）' AFTER properties,
    ADD COLUMN art_style_description   TEXT          NULL COMMENT '画风中文描述' AFTER art_style,
    ADD COLUMN art_style_image_prompt  TEXT          NULL COMMENT '画风英文提示词' AFTER art_style_description,
    ADD COLUMN art_style_image_url     VARCHAR(1024) NULL COMMENT '画风参考图路径' AFTER art_style_image_prompt;
