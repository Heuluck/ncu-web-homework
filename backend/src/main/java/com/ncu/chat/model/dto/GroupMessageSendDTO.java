package com.ncu.chat.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupMessageSendDTO {
    @NotNull(message = "群ID不能为空")
    private Long groupId;
    @NotNull(message = "消息内容不能为空")
    private String content;
    private Integer messageType;
    private String fileUrl;
}