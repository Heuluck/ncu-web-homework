package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_bot")
public class GroupBot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long botId;
    private Long addedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
