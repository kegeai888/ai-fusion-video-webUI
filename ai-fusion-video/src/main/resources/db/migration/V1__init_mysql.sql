-- ==========================================================
-- V1: 初始化 MySQL 数据库 (合并 V1~V10)
-- ==========================================================

-- ========== 系统表 ==========

CREATE TABLE sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username        VARCHAR(64)     NOT NULL UNIQUE COMMENT '登录用户名（唯一）',
    password        VARCHAR(128)    NOT NULL COMMENT '登录密码（BCrypt加密）',
    nickname        VARCHAR(64)     NULL COMMENT '用户昵称',
    avatar          VARCHAR(512)    NULL COMMENT '头像URL',
    email           VARCHAR(128)    NULL COMMENT '邮箱地址',
    phone           VARCHAR(20)     NULL COMMENT '手机号码',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '用户表';

CREATE TABLE sys_role (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(64)     NOT NULL COMMENT '角色名称',
    code            VARCHAR(64)     NOT NULL UNIQUE COMMENT '角色代码标识（如 admin、user）',
    sort            INT             DEFAULT 0 COMMENT '排列顺序',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    remark          VARCHAR(512)    NULL COMMENT '备注说明',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '角色表';

CREATE TABLE sys_user_role (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT '用户角色关联表';

-- ========== 团队 ==========

CREATE TABLE afv_team (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(128)    NOT NULL COMMENT '团队名称',
    logo            VARCHAR(512)    NULL COMMENT '团队LOGO图片URL',
    description     VARCHAR(1024)   NULL COMMENT '团队描述',
    owner_user_id   BIGINT          NOT NULL COMMENT '创建者用户ID',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '团队表';

CREATE TABLE afv_team_member (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    team_id         BIGINT          NOT NULL COMMENT '所属团队ID',
    user_id         BIGINT          NOT NULL COMMENT '成员用户ID',
    role            INT             NOT NULL DEFAULT 3 COMMENT '角色：1-创建者 2-管理员 3-普通成员',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    join_time       DATETIME        NULL COMMENT '加入时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_team_user (team_id, user_id)
) COMMENT '团队成员表';

-- ========== AI 模型 & API 配置 ==========

CREATE TABLE afv_ai_model (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(128)    NOT NULL COMMENT '模型显示名称',
    code            VARCHAR(64)     NOT NULL UNIQUE COMMENT '模型代码标识（如 deepseek-chat、qwen-vl-max）',
    model_type      INT             NOT NULL COMMENT '模型类型：1-文本对话 2-图片生成 3-视频生成',
    icon            VARCHAR(512)    NULL COMMENT '模型图标URL',
    description     VARCHAR(1024)   NULL COMMENT '模型描述说明',
    sort            INT             DEFAULT 0 COMMENT '排列顺序',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    config          TEXT            NULL COMMENT '模型特定配置JSON（temperature、top_p等）',
    default_model   TINYINT         DEFAULT 0 COMMENT '是否为默认模型',
    max_concurrency INT             DEFAULT 5 COMMENT '最大并发请求数',
    api_config_id   BIGINT          NULL COMMENT '关联API配置ID',
    support_vision  TINYINT         DEFAULT 0 COMMENT '是否支持视觉理解（传图片）',
    support_reasoning TINYINT       DEFAULT 0 COMMENT '是否支持深度思考（reasoning）',
    context_window  INT             NULL COMMENT '上下文窗口大小（token数）',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT 'AI模型表';

CREATE TABLE afv_api_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(128)    NOT NULL COMMENT '配置名称',
    platform        VARCHAR(32)     NULL COMMENT '平台标识：deepseek/dashscope/openai_compatible/ollama/anthropic/vertex_ai',
    api_type        INT             NULL COMMENT 'API类型：1-文本对话 2-图片生成 3-视频生成',
    api_url         VARCHAR(512)    NULL COMMENT 'API接口地址',
    api_key         VARCHAR(512)    NULL COMMENT 'API密钥',
    app_id          VARCHAR(128)    NULL COMMENT '应用ID（部分平台需要）',
    app_secret      VARCHAR(512)    NULL COMMENT '应用密钥（部分平台需要）',
    model_id        BIGINT          NULL COMMENT '关联模型ID',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    remark          VARCHAR(1024)   NULL COMMENT '备注说明',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT 'API配置表';

-- ========== Agent 对话 ==========

CREATE TABLE afv_agent_conversation (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    conversation_id     VARCHAR(64)     NOT NULL UNIQUE COMMENT '对话唯一标识（UUID）',
    user_id             BIGINT          NULL COMMENT '所属用户ID',
    project_id          BIGINT          NULL COMMENT '关联项目ID',
    context_type        VARCHAR(50)     NULL COMMENT '上下文类型（project/script/storyboard）',
    agent_type          VARCHAR(64)     NULL COMMENT 'Agent类型（script_parser/storyboard_creator）',
    category            VARCHAR(32)     NULL COMMENT '对话分类标签',
    context_id          BIGINT          NULL COMMENT '上下文对象ID',
    title               VARCHAR(255)    DEFAULT '新对话' COMMENT '对话标题',
    message_count       INT             DEFAULT 0 COMMENT '消息总数',
    last_message_time   DATETIME        NULL COMMENT '最后消息时间',
    status              VARCHAR(32)     NULL COMMENT '对话状态：active/closed',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_conv_project_context (project_id, context_type, context_id),
    INDEX idx_conv_user (user_id)
) COMMENT 'Agent对话索引表';

CREATE TABLE afv_agent_message (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    conversation_id       VARCHAR(64)     NOT NULL COMMENT '所属对话ID（UUID）',
    role                  VARCHAR(20)     NOT NULL COMMENT '消息角色：user/assistant/system/tool',
    content               TEXT            NULL COMMENT '消息文本内容',
    references_json       TEXT            NULL COMMENT '引用资源JSON',
    tool_name             VARCHAR(100)    NULL COMMENT '工具调用名称（role=tool时）',
    tool_status           VARCHAR(20)     NULL COMMENT '工具执行状态：running/success/error',
    tool_call_id          VARCHAR(128)    NULL COMMENT '工具调用ID（关联同一次调用的发起和结果）',
    parent_tool_call_id   VARCHAR(128)    NULL COMMENT '父级工具调用ID（子Agent事件归属到父工具调用）',
    reasoning_content     TEXT            NULL COMMENT 'AI推理过程内容（思维链）',
    reasoning_duration_ms BIGINT          NULL COMMENT 'AI推理耗时（毫秒）',
    message_order         INT             NOT NULL COMMENT '消息排列顺序',
    deleted               TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_msg_conversation (conversation_id),
    INDEX idx_msg_conv_order (conversation_id, message_order)
) COMMENT 'Agent消息表';

-- ========== 生图 ==========

CREATE TABLE afv_image_task (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id            VARCHAR(64)     NOT NULL UNIQUE COMMENT '任务唯一标识',
    user_id            BIGINT          NOT NULL COMMENT '发起用户ID',
    project_id         BIGINT          NULL COMMENT '关联项目ID',
    prompt             TEXT            NULL COMMENT '生图提示词',
    prompt_template_id BIGINT          NULL COMMENT '提示词模板ID',
    ref_image_urls     JSON            NULL COMMENT '参考图片URL列表JSON',
    ratio              VARCHAR(32)     NULL COMMENT '画面比例（如16:9）',
    resolution         VARCHAR(16)     NULL COMMENT '分辨率（如1920x1080）',
    aspect_ratio       VARCHAR(16)     NULL COMMENT '宽高比描述',
    width              INT             NULL COMMENT '图片宽度（像素）',
    height             INT             NULL COMMENT '图片高度（像素）',
    count              INT             DEFAULT 1 COMMENT '生成数量',
    success_count      INT             DEFAULT 0 COMMENT '已成功生成数量',
    status             INT             DEFAULT 0 COMMENT '任务状态：0-排队中 1-处理中 2-已完成 3-失败',
    error_msg          TEXT            NULL COMMENT '失败错误信息',
    model_id           BIGINT          NULL COMMENT '使用的AI模型ID',
    category           VARCHAR(64)     NULL COMMENT '任务分类标签',
    owner_type         INT             NULL COMMENT '拥有者类型：1-个人 2-团队',
    owner_id           BIGINT          NULL COMMENT '拥有者ID',
    deleted            TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '生图任务表';

CREATE TABLE afv_image_item (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id          BIGINT          NOT NULL COMMENT '所属生图任务ID',
    platform_task_id VARCHAR(128)    NULL COMMENT '平台侧任务ID',
    image_url        VARCHAR(1024)   NULL COMMENT '生成的图片URL',
    thumbnail_url    VARCHAR(1024)   NULL COMMENT '缩略图URL',
    width            INT             NULL COMMENT '图片宽度（像素）',
    height           INT             NULL COMMENT '图片高度（像素）',
    file_size        BIGINT          NULL COMMENT '文件大小（字节）',
    status           INT             DEFAULT 0 COMMENT '状态：0-生成中 1-成功 2-失败',
    error_msg        TEXT            NULL COMMENT '失败错误信息',
    deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_image_item_task (task_id)
) COMMENT '生图条目表';

-- ========== 生视频 ==========

CREATE TABLE afv_video_task (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id                VARCHAR(64)     NOT NULL UNIQUE COMMENT '任务唯一标识',
    user_id                BIGINT          NOT NULL COMMENT '发起用户ID',
    project_id             BIGINT          NULL COMMENT '关联项目ID',
    prompt                 TEXT            NULL COMMENT '生视频提示词',
    prompt_template_id     BIGINT          NULL COMMENT '提示词模板ID',
    generate_mode          VARCHAR(32)     NULL COMMENT '生成模式：text2video/image2video',
    first_frame_image_url  VARCHAR(1024)   NULL COMMENT '首帧参考图片URL',
    last_frame_image_url   VARCHAR(1024)   NULL COMMENT '尾帧参考图片URL',
    reference_image_urls   JSON            NULL COMMENT '参考图片URL列表JSON',
    ratio                  VARCHAR(32)     NULL COMMENT '画面比例（如16:9）',
    resolution             VARCHAR(16)     NULL COMMENT '分辨率（如1920x1080）',
    duration               INT             NULL COMMENT '视频时长（秒）',
    watermark              TINYINT         DEFAULT 0 COMMENT '是否添加水印',
    generate_audio         TINYINT         DEFAULT 0 COMMENT '是否生成配音',
    seed                   BIGINT          NULL COMMENT '随机种子（用于复现）',
    camera_fixed           TINYINT         DEFAULT 0 COMMENT '是否固定镜头',
    count                  INT             DEFAULT 1 COMMENT '生成数量',
    success_count          INT             DEFAULT 0 COMMENT '已成功生成数量',
    status                 INT             DEFAULT 0 COMMENT '任务状态：0-排队中 1-处理中 2-已完成 3-失败',
    error_msg              TEXT            NULL COMMENT '失败错误信息',
    model_id               BIGINT          NULL COMMENT '使用的AI模型ID',
    category               VARCHAR(64)     NULL COMMENT '任务分类标签',
    owner_type             INT             NULL COMMENT '拥有者类型：1-个人 2-团队',
    owner_id               BIGINT          NULL COMMENT '拥有者ID',
    deleted                TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '生视频任务表';

CREATE TABLE afv_video_item (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id          BIGINT          NOT NULL COMMENT '所属生视频任务ID',
    platform_task_id VARCHAR(128)    NULL COMMENT '平台侧任务ID',
    video_url        VARCHAR(1024)   NULL COMMENT '生成的视频URL',
    cover_url        VARCHAR(1024)   NULL COMMENT '视频封面图URL',
    duration         INT             NULL COMMENT '视频时长（秒）',
    file_size        BIGINT          NULL COMMENT '文件大小（字节）',
    status           INT             DEFAULT 0 COMMENT '状态：0-生成中 1-成功 2-失败',
    error_msg        TEXT            NULL COMMENT '失败错误信息',
    first_frame_url  VARCHAR(1024)   NULL COMMENT '视频首帧图片URL',
    last_frame_url   VARCHAR(1024)   NULL COMMENT '视频尾帧图片URL',
    deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_video_item_task (task_id)
) COMMENT '生视频条目表';

-- ========== 项目 ==========

CREATE TABLE afv_project (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(256)    NOT NULL COMMENT '项目名称',
    description     TEXT            NULL COMMENT '项目描述',
    cover_url       VARCHAR(1024)   NULL COMMENT '项目封面图URL',
    scope           INT             DEFAULT 2 COMMENT '可见范围：1-公开 2-私有 3-仅团队可见',
    owner_type      INT             NOT NULL COMMENT '拥有者类型：1-个人 2-团队',
    owner_id        BIGINT          NOT NULL COMMENT '拥有者ID',
    status          INT             DEFAULT 0 COMMENT '状态：0-筹备中 1-进行中 2-已完成 3-已归档',
    properties      JSON            NULL COMMENT '扩展配置JSON',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '视频项目表';

CREATE TABLE afv_project_member (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id      BIGINT          NOT NULL COMMENT '所属项目ID',
    user_id         BIGINT          NOT NULL COMMENT '成员用户ID',
    role            INT             NOT NULL DEFAULT 3 COMMENT '成员角色：1-拥有者 2-管理员 3-普通成员',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_project_user (project_id, user_id)
) COMMENT '项目成员表';

-- ========== 剧本 ==========

CREATE TABLE afv_script (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id          BIGINT          NOT NULL COMMENT '所属项目ID',
    title               VARCHAR(256)    NULL COMMENT '剧本标题',
    content             TEXT            NULL COMMENT '剧本正文内容（格式化后）',
    raw_content         TEXT            NULL COMMENT '剧本原始内容（用户粘贴的原文）',
    total_episodes      INT             DEFAULT 0 COMMENT '总集数',
    story_synopsis      TEXT            NULL COMMENT '故事梗概',
    characters_json     JSON            NULL COMMENT '角色列表JSON',
    source_type         INT             DEFAULT 0 COMMENT '来源类型：0-手动创建 1-文件导入 2-AI生成',
    parsing_status      INT             DEFAULT 0 COMMENT '解析状态：0-未解析 1-解析中 2-解析完成 3-解析失败',
    parsing_progress    VARCHAR(256)    NULL COMMENT '解析进度描述',
    summary             TEXT            NULL COMMENT 'AI生成的剧本摘要',
    genre               VARCHAR(128)    NULL COMMENT '剧本类型/题材',
    target_audience     VARCHAR(128)    NULL COMMENT '目标受众',
    duration_estimate   INT             NULL COMMENT '预估总时长（分钟）',
    scope               INT             DEFAULT 3 COMMENT '可见范围：1-公开 2-私有 3-仅团队可见',
    owner_type          INT             NULL COMMENT '拥有者类型：1-个人 2-团队',
    owner_id            BIGINT          NULL COMMENT '拥有者ID',
    ai_generated        TINYINT         DEFAULT 0 COMMENT '是否由AI生成',
    version             INT             DEFAULT 0 COMMENT '乐观锁版本号',
    status              INT             DEFAULT 0 COMMENT '状态：0-草稿 1-正常',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_script_project (project_id)
) COMMENT '剧本表';

CREATE TABLE afv_script_episode (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    script_id           BIGINT          NOT NULL COMMENT '所属剧本ID',
    episode_number      INT             NULL COMMENT '集号（从1开始）',
    title               VARCHAR(256)    NULL COMMENT '本集标题',
    synopsis            TEXT            NULL COMMENT '本集剧情梗概',
    raw_content         TEXT            NULL COMMENT '本集原始剧本内容',
    duration_estimate   INT             NULL COMMENT '预估时长（分钟）',
    total_scenes        INT             DEFAULT 0 COMMENT '本集总场次数',
    source_type         INT             DEFAULT 0 COMMENT '来源类型：0-AI解析 1-手动添加',
    sort_order          INT             DEFAULT 0 COMMENT '排列顺序',
    parsing_status      INT             DEFAULT 0 COMMENT '解析状态：0-未解析 1-解析中 2-解析完成 3-解析失败',
    status              INT             DEFAULT 0 COMMENT '状态：0-草稿 1-正常',
    version             INT             DEFAULT 0 COMMENT '乐观锁版本号',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_episode_script (script_id)
) COMMENT '分集剧本表';

CREATE TABLE afv_script_scene_item (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    episode_id          BIGINT          NOT NULL COMMENT '所属分集ID',
    script_id           BIGINT          NOT NULL COMMENT '所属剧本ID',
    scene_number        VARCHAR(32)     NULL COMMENT '场次编号（如1-1表示第1集第1场）',
    scene_heading       VARCHAR(256)    NULL COMMENT '场景标头（如"内景 客厅 夜"）',
    location            VARCHAR(128)    NULL COMMENT '场景地点',
    time_of_day         VARCHAR(32)     NULL COMMENT '时间段：日/夜/黄昏/清晨等',
    int_ext             VARCHAR(16)     NULL COMMENT '内外景标识：内景/外景/内外景',
    characters          JSON            NULL COMMENT '出场角色名列表JSON',
    character_asset_ids JSON            NULL COMMENT '出场角色资产ID列表JSON',
    scene_asset_id      BIGINT          NULL COMMENT '场景资产ID',
    prop_asset_ids      JSON            NULL COMMENT '道具资产ID列表JSON',
    scene_description   TEXT            NULL COMMENT '场景氛围/环境描述',
    dialogues           JSON            NULL COMMENT '对白/动作元素列表JSON',
    sort_order          INT             DEFAULT 0 COMMENT '排列顺序',
    status              INT             DEFAULT 0 COMMENT '状态：0-草稿 1-正常',
    version             INT             DEFAULT 0 COMMENT '乐观锁版本号',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_script_scene_episode (episode_id),
    INDEX idx_script_scene_script (script_id)
) COMMENT '剧本分场次表';

-- ========== 分镜 ==========

CREATE TABLE afv_storyboard (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id          BIGINT          NULL COMMENT '所属项目ID',
    script_id           BIGINT          NULL COMMENT '关联剧本ID',
    title               VARCHAR(256)    NULL COMMENT '分镜标题',
    description         TEXT            NULL COMMENT '分镜描述',
    custom_columns      JSON            NULL COMMENT '自定义列配置JSON',
    scope               INT             DEFAULT 3 COMMENT '可见范围：1-公开 2-私有 3-仅团队可见',
    owner_type          INT             NULL COMMENT '拥有者类型：1-个人 2-团队',
    owner_id            BIGINT          NULL COMMENT '拥有者ID',
    total_duration      INT             NULL COMMENT '预估总时长（秒）',
    status              INT             DEFAULT 0 COMMENT '状态：0-草稿 1-正常',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_storyboard_project (project_id)
) COMMENT '分镜脚本表';

CREATE TABLE afv_storyboard_episode (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    storyboard_id       BIGINT          NOT NULL COMMENT '所属分镜ID',
    episode_number      INT             NULL COMMENT '集号',
    title               VARCHAR(200)    NULL COMMENT '集标题',
    synopsis            TEXT            NULL COMMENT '本集梗概',
    sort_order          INT             DEFAULT 0 COMMENT '排列顺序',
    status              INT             DEFAULT 0 COMMENT '状态：0-草稿 1-正常',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除标志',
    create_time         DATETIME        NULL COMMENT '创建时间',
    update_time         DATETIME        NULL COMMENT '更新时间',
    INDEX idx_sb_episode_storyboard (storyboard_id)
) COMMENT '分镜集表';

CREATE TABLE afv_storyboard_scene (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    episode_id          BIGINT          NOT NULL COMMENT '所属分镜集ID',
    storyboard_id       BIGINT          NOT NULL COMMENT '所属分镜ID（冗余）',
    scene_number        VARCHAR(20)     NULL COMMENT '场次编号',
    scene_heading       VARCHAR(200)    NULL COMMENT '场景标头',
    location            VARCHAR(200)    NULL COMMENT '场景地点',
    time_of_day         VARCHAR(20)     NULL COMMENT '时间段',
    int_ext             VARCHAR(20)     NULL COMMENT '内外景标识',
    sort_order          INT             DEFAULT 0 COMMENT '排列顺序',
    status              INT             DEFAULT 0 COMMENT '状态：0-草稿 1-正常',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除标志',
    create_time         DATETIME        NULL COMMENT '创建时间',
    update_time         DATETIME        NULL COMMENT '更新时间',
    INDEX idx_sb_scene_episode (episode_id),
    INDEX idx_sb_scene_storyboard (storyboard_id)
) COMMENT '分镜场次表';

CREATE TABLE afv_storyboard_item (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    storyboard_id         BIGINT          NOT NULL COMMENT '所属分镜ID',
    storyboard_episode_id BIGINT          NULL COMMENT '所属分镜集ID',
    storyboard_scene_id   BIGINT          NULL COMMENT '所属分镜场次ID',
    sort_order            INT             DEFAULT 0 COMMENT '排列顺序',
    shot_number           VARCHAR(32)     NULL COMMENT '镜号',
    auto_shot_number      VARCHAR(32)     NULL COMMENT '自动编号（系统生成）',
    image_url             VARCHAR(1024)   NULL COMMENT '用户上传参考图片URL',
    reference_image_url   VARCHAR(1024)   NULL COMMENT '外部参考图片URL',
    video_url             VARCHAR(1024)   NULL COMMENT '视频URL（最终成品）',
    generated_image_url   VARCHAR(1024)   NULL COMMENT 'AI生成的图片URL',
    generated_video_url   VARCHAR(1024)   NULL COMMENT 'AI生成的视频URL',
    shot_type             VARCHAR(32)     NULL COMMENT '景别：远景/全景/中景/近景/特写',
    duration              DECIMAL(10,2)   NULL COMMENT '预估时长（秒）',
    content               TEXT            NULL COMMENT '画面内容描述',
    scene_expectation     TEXT            NULL COMMENT '画面期望描述（AI生图提示）',
    sound                 TEXT            NULL COMMENT '声音描述',
    dialogue              TEXT            NULL COMMENT '台词/旁白',
    sound_effect          VARCHAR(512)    NULL COMMENT '音效',
    music                 VARCHAR(512)    NULL COMMENT '配乐建议',
    camera_movement       VARCHAR(64)     NULL COMMENT '镜头运动：推/拉/摇/移/跟/升/降',
    camera_angle          VARCHAR(64)     NULL COMMENT '镜头角度：平视/俯视/仰视',
    camera_equipment      VARCHAR(64)     NULL COMMENT '摄像机装备',
    focal_length          VARCHAR(64)     NULL COMMENT '镜头焦段',
    transition            VARCHAR(64)     NULL COMMENT '转场效果：切/淡入/淡出/溶/划',
    character_ids         JSON            NULL COMMENT '出场角色资产ID列表JSON',
    scene_id              BIGINT          NULL COMMENT '引用场景资产ID',
    remark                TEXT            NULL COMMENT '备注',
    custom_data           JSON            NULL COMMENT '自定义扩展数据JSON',
    ai_generated          TINYINT         DEFAULT 0 COMMENT '是否由AI生成',
    status                INT             DEFAULT 0 COMMENT '状态：0-草稿 1-正常',
    deleted               TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sb_item_storyboard (storyboard_id),
    INDEX idx_sb_item_scene (storyboard_scene_id),
    INDEX idx_sb_item_episode (storyboard_episode_id)
) COMMENT '分镜条目表';

-- ========== 资产 ==========

CREATE TABLE afv_asset (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         BIGINT          NULL COMMENT '创建者用户ID',
    project_id      BIGINT          NULL COMMENT '所属项目ID',
    type            VARCHAR(32)     NULL COMMENT '资产类型：character/scene/prop/vehicle/building/costume/effect',
    name            VARCHAR(128)    NOT NULL COMMENT '资产名称',
    description     TEXT            NULL COMMENT '资产描述',
    cover_url       VARCHAR(1024)   NULL COMMENT '封面图URL',
    properties      JSON            NULL COMMENT '动态属性JSON（如角色的appearance、age等）',
    tags            JSON            NULL COMMENT '标签列表JSON',
    source_type     INT             DEFAULT 1 COMMENT '来源类型：1-用户上传 2-AI生成',
    ai_prompt       TEXT            NULL COMMENT 'AI生成时使用的提示词',
    owner_type      INT             NULL COMMENT '拥有者类型：1-个人 2-团队',
    owner_id        BIGINT          NULL COMMENT '拥有者ID',
    status          INT             DEFAULT 1 COMMENT '状态：0-草稿 1-正常',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_asset_project (project_id),
    INDEX idx_asset_owner (owner_type, owner_id),
    INDEX idx_asset_type (project_id, type)
) COMMENT '资产表';

CREATE TABLE afv_asset_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    asset_id        BIGINT          NOT NULL COMMENT '所属主资产ID',
    item_type       VARCHAR(32)     NULL COMMENT '子资产类型：front/side/back/detail/expression/pose/variant/original',
    name            VARCHAR(128)    NULL COMMENT '子资产名称',
    image_url       VARCHAR(1024)   NULL COMMENT '图片URL',
    thumbnail_url   VARCHAR(1024)   NULL COMMENT '缩略图URL',
    properties      JSON            NULL COMMENT '动态属性JSON',
    sort_order      INT             DEFAULT 0 COMMENT '排列顺序',
    source_type     INT             DEFAULT 1 COMMENT '来源类型：1-用户上传 2-AI生成',
    ai_prompt       TEXT            NULL COMMENT 'AI生成时使用的提示词',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_asset_item_asset (asset_id)
) COMMENT '子资产表';

-- ========== 初始化数据 ==========

INSERT INTO sys_role (name, code, sort, status, remark) VALUES
('超级管理员', 'admin', 1, 1, '系统超级管理员'),
('普通用户', 'user', 2, 1, '默认用户角色');
