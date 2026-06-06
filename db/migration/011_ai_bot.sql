USE chat_system;

-- AI 机器人定义表
CREATE TABLE IF NOT EXISTS `ai_bot` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `owner_id` BIGINT NOT NULL COMMENT '创建者 userId',
  `name` VARCHAR(50) NOT NULL COMMENT '机器人名称',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
  `endpoint` VARCHAR(500) NOT NULL COMMENT 'OpenAI 兼容 API 地址',
  `api_key_encrypted` TEXT NOT NULL COMMENT 'AES 加密的 API Key',
  `model` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `system_prompt` TEXT COMMENT '系统提示词',
  `trigger_type` TINYINT DEFAULT 0 COMMENT '触发条件：0=@触发 1=每次触发 2=随机概率',
  `trigger_probability` DECIMAL(5,4) DEFAULT NULL COMMENT '随机概率值（0~1）',
  `temperature` DECIMAL(3,2) DEFAULT 1.00 COMMENT '生成温度',
  `top_p` DECIMAL(3,2) DEFAULT 1.00 COMMENT 'top_p 采样',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  INDEX idx_owner (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 群聊-机器人关联表
CREATE TABLE IF NOT EXISTS `group_bot` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL COMMENT '群聊 ID',
  `bot_id` BIGINT NOT NULL COMMENT '机器人 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_group_bot (`group_id`, `bot_id`),
  INDEX idx_group_id (`group_id`),
  INDEX idx_bot_id (`bot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- group_bot 表新增 added_by 字段（幂等）
SET @col_added_by = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'chat_system' AND table_name = 'group_bot' AND column_name = 'added_by'
);
SET @sql_added_by = IF(@col_added_by = 0,
  'ALTER TABLE `group_bot` ADD COLUMN `added_by` BIGINT NOT NULL COMMENT "添加者 userId" AFTER `bot_id`',
  'SELECT 1'
);
PREPARE stmt_ab FROM @sql_added_by;
EXECUTE stmt_ab;
DEALLOCATE PREPARE stmt_ab;

-- group_message 表新增 bot_id 字段（幂等：仅当列不存在时添加）
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'chat_system' AND table_name = 'group_message' AND column_name = 'bot_id'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `group_message` ADD COLUMN `bot_id` BIGINT DEFAULT NULL COMMENT "机器人 ID（非机器人消息为 NULL）" AFTER `sender_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
