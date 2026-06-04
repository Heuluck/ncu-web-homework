-- 1. 群聊表
CREATE TABLE IF NOT EXISTS `chat_group` (
                                            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            `name` VARCHAR(100) NOT NULL COMMENT '群名称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '群头像',
    `announcement` VARCHAR(500) DEFAULT NULL COMMENT '群公告',
    `owner_id` BIGINT NOT NULL COMMENT '群主 user_id',
    `member_count` INT DEFAULT 0 COMMENT '成员数量',
    `max_members` INT DEFAULT 500 COMMENT '最大成员数',
    `created_by` BIGINT NOT NULL COMMENT '创建者 user_id',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_owner` (`owner_id`),
    INDEX `idx_name` (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊表';

-- 2. 群成员表
CREATE TABLE IF NOT EXISTS `group_member` (
                                              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              `group_id` BIGINT NOT NULL COMMENT '群ID',
                                              `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                              `role` TINYINT DEFAULT 0 COMMENT '角色: 0-普通成员 1-管理员 2-群主',
                                              `nickname` VARCHAR(50) DEFAULT NULL COMMENT '群内昵称',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_read_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `mute_expire_time` DATETIME DEFAULT NULL,
    `do_not_disturb` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_group_id` (`group_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员表';

-- 3. 群消息表
CREATE TABLE IF NOT EXISTS `group_message` (
                                               `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               `group_id` BIGINT NOT NULL,
                                               `sender_id` BIGINT NOT NULL,
                                               `content` TEXT NOT NULL,
                                               `message_type` TINYINT DEFAULT 0,
                                               `file_url` VARCHAR(500) DEFAULT NULL,
    `is_recall` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_group_time` (`group_id`, `create_time`),
    INDEX `idx_sender` (`sender_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群消息表';