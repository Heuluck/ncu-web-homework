USE chat_system;

-- group_bot 表新增 added_by 字段，记录谁将机器人添加到群聊（幂等：列已存在则跳过）
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'chat_system' AND TABLE_NAME = 'group_bot' AND COLUMN_NAME = 'added_by'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `group_bot` ADD COLUMN `added_by` BIGINT DEFAULT NULL COMMENT ''添加者 userId（群主或管理员）'' AFTER `bot_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
