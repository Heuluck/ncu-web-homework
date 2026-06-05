package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("voice_message")
public class VoiceMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long groupId;
    private String fileUrl;
    private Integer duration;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
