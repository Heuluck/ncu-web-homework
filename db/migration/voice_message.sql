USE chat_system;

CREATE TABLE IF NOT EXISTS `voice_message` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT DEFAULT NULL COMMENT '接收者ID（私聊）',
  `group_id` BIGINT DEFAULT NULL COMMENT '群ID（群聊）',
  `file_url` VARCHAR(512) NOT NULL COMMENT '语音文件路径',
  `duration` INT NOT NULL DEFAULT 0 COMMENT '语音时长（秒）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  INDEX `idx_sender_id` (`sender_id`),
  INDEX `idx_receiver_id` (`receiver_id`),
  INDEX `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音消息表';
