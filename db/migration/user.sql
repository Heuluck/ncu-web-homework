USE chat_system;

CREATE TABLE IF NOT EXISTS `user` (
                                      `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                                      `password` VARCHAR(255) NOT NULL COMMENT '密码',
                                      `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
                                      `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
                                      `signature` VARCHAR(200) DEFAULT NULL COMMENT '个性签名',
                                      `status` TINYINT DEFAULT 1 COMMENT '状态: 1-在线 2-离线 3-忙碌 4-勿扰',
                                      `role` TINYINT DEFAULT 0 COMMENT '角色: 0-普通用户 1-管理员',
                                      `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
                                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                      `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';