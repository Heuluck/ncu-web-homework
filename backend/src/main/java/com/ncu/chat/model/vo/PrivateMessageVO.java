package com.ncu.chat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PrivateMessageVO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer messageType;
    private Integer status;
    private String fileUrl;
    private LocalDateTime createTime;
    private String senderNickname;
    private String senderAvatar;
}
