USE chat_system;

-- 文件资源表（记录上传的文件/图片/语音元信息）
CREATE TABLE IF NOT EXISTS `file_resource` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件访问 URL',
    `file_path` VARCHAR(500) NOT NULL COMMENT '服务器存储路径',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件 MIME 类型',
    `uploader_id` BIGINT NOT NULL COMMENT '上传者 user_id',
    `category` TINYINT DEFAULT 0 COMMENT '分类：0-通用文件 1-图片 2-音频',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_uploader` (`uploader_id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源表';
