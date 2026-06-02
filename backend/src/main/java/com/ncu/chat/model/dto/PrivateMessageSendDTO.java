package com.ncu.chat.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrivateMessageSendDTO {
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;
    @NotNull(message = "消息内容不能为空")
    private String content;
    private Integer messageType = 0;  // 默认文字
    private String fileUrl;
}
