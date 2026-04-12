-- 分镜条目资产关联字段修正
-- 1. 删除旧的 scene_id 列（原本语义混乱，不再使用）
ALTER TABLE afv_storyboard_item
  DROP COLUMN scene_id;

-- 2. 新增场景子资产ID
ALTER TABLE afv_storyboard_item
  ADD COLUMN scene_asset_item_id bigint DEFAULT NULL COMMENT '场景子资产ID (AssetItem.id)' AFTER character_ids;

-- 3. 新增道具子资产ID列表
ALTER TABLE afv_storyboard_item
  ADD COLUMN prop_ids text DEFAULT NULL COMMENT '道具子资产ID列表 JSON (List<Long> of AssetItem.id)' AFTER scene_asset_item_id;

-- 4. 更新 character_ids 注释（字段名不变，语义改为子资产ID）
ALTER TABLE afv_storyboard_item
  MODIFY COLUMN character_ids text COMMENT '出场角色子资产ID列表 JSON (List<Long> of AssetItem.id)';
