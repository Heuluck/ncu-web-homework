package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("friendship")
public class Friendship {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requesterId;
    private Long receiverId;
    private Integer status;    // 0-待处理 1-已接受 2-已拒绝
    private Integer requesterBlocked; // 发起者是否拉黑了对方
    private Integer receiverBlocked;  // 接收者是否拉黑了对方
    private Long requesterGroupId;
    private Long receiverGroupId;
    private String verificationMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
