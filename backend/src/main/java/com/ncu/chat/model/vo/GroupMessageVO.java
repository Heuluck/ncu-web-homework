package com.ncu.chat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupMessageVO {
    private Long id;
    private Long groupId;
    private Long senderId;
    private Long botId;
    private String botName;
    private String botAvatar;
    private String senderNickname;
    private String senderAvatar;
    private String content;
    private Integer messageType;
    private String fileUrl;
    private Boolean isSelf;
    private Boolean isRecall;
    private LocalDateTime createTime;
}