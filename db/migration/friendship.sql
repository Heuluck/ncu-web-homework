USE chat_system;

CREATE TABLE IF NOT EXISTS `friendship` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `requester_id` BIGINT NOT NULL COMMENT '发起者用户ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者用户ID',
    `status` TINYINT DEFAULT 0 COMMENT '0-待处理 1-已接受 2-已拒绝 3-已拉黑',
    `requester_blocked` TINYINT DEFAULT 0 COMMENT '发起者是否拉黑对方',
    `receiver_blocked` TINYINT DEFAULT 0 COMMENT '接收者是否拉黑对方',
    `requester_group_id` BIGINT DEFAULT NULL COMMENT '发起者的分组ID',
    `receiver_group_id` BIGINT DEFAULT NULL COMMENT '接收者的分组ID',
    `verification_message` VARCHAR(255) DEFAULT NULL COMMENT '验证信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_requester` (`requester_id`),
    INDEX `idx_receiver` (`receiver_id`),
    INDEX `idx_requester_status` (`requester_id`, `status`),
    INDEX `idx_receiver_status` (`receiver_id`, `status`),
    UNIQUE KEY `uk_friendship` (`requester_id`, `receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';
