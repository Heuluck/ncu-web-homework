USE chat_system;

-- 表情表
DROP TABLE IF EXISTS `emoji`;
CREATE TABLE IF NOT EXISTS `emoji` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    `name` VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL COMMENT '表情名称',
    `url` VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL COMMENT '表情符号',
    `category` VARCHAR(50) DEFAULT 'default' COMMENT '表情分类',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_category` (`category`),
    INDEX `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表情表';

-- 初始化默认表情数据（使用原生 emoji）
INSERT INTO `emoji` (`name`, `url`, `category`, `sort_order`) VALUES
('微笑', '😀', 'default', 1),
('大笑', '😂', 'default', 2),
('开心', '😊', 'default', 3),
('眨眼', '😉', 'default', 4),
('可爱', '🥰', 'default', 5),
('思考', '🤔', 'default', 6),
('惊讶', '😮', 'default', 7),
('难过', '😢', 'default', 8),
('哭泣', '😭', 'default', 9),
('生气', '😠', 'default', 10),
('爱心', '❤️', 'default', 11),
('点赞', '👍', 'default', 12),
('加油', '💪', 'default', 13),
('OK', '👌', 'default', 14),
('握手', '🤝', 'default', 15),
('拥抱', '🤗', 'default', 16),
('玫瑰', '🌹', 'default', 17),
('蛋糕', '🎂', 'default', 18),
('礼物', '🎁', 'default', 19),
('烟花', '🎉', 'default', 20),
('飞吻', '😘', 'default', 21),
('得意', '😎', 'default', 22),
('困', '😴', 'default', 23),
('晕', '😵', 'default', 24),
('鼓掌', '👏', 'default', 25),
('笑哭', '🤣', 'default', 26),
('撇嘴', '😏', 'default', 27),
('害羞', '😳', 'default', 28),
('闭嘴', '🤐', 'default', 29),
('再见', '👋', 'default', 30);
