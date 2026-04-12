-- 为视频生成任务表新增参考视频和参考音频URL列表字段（Seedance 2.0 多模态参考能力）
ALTER TABLE afv_video_task ADD COLUMN reference_video_urls JSON NULL COMMENT '参考视频URL列表 JSON' AFTER reference_image_urls;
ALTER TABLE afv_video_task ADD COLUMN reference_audio_urls JSON NULL COMMENT '参考音频URL列表 JSON' AFTER reference_video_urls;
