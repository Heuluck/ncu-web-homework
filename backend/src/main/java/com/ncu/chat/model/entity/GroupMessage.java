package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_message")
public class GroupMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long senderId;
    private String content;
    private Integer messageType;
    private String fileUrl;
    private Integer isRecall;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}