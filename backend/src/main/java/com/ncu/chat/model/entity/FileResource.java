package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件资源实体类
 */
@Data
@TableName("file_resource")
public class FileResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件访问 URL
     */
    private String fileUrl;

    /**
     * 服务器存储路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件 MIME 类型
     */
    private String fileType;

    /**
     * 上传者 user_id
     */
    private Long uploaderId;

    /**
     * 分类：0-通用文件 1-图片 2-音频
     */
    private Integer category;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
