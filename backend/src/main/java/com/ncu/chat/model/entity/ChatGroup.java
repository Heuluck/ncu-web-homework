package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_group")
public class ChatGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private String announcement;
    private Long ownerId;
    private Integer memberCount;
    private Integer maxMembers;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}