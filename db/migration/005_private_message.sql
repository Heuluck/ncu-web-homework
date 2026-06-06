USE chat_system;

CREATE TABLE IF NOT EXISTS `private_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` BIGINT NOT NULL COMMENT '发送者 user_id',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者 user_id',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `message_type` TINYINT DEFAULT 0 COMMENT '0-文字 1-图片 2-文件 3-语音',
    `status` TINYINT DEFAULT 0 COMMENT '0-未读 1-已读',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件/图片/语音 URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_sender_receiver_time` (`sender_id`, `receiver_id`, `create_time`),
    INDEX `idx_receiver_status` (`receiver_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
