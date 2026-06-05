package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 表情实体类
 */
@Data
@TableName("emoji")
public class Emoji {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 表情名称
     */
    private String name;

    /**
     * 表情图片 URL
     */
    private String url;

    /**
     * 表情分类
     */
    private String category;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
