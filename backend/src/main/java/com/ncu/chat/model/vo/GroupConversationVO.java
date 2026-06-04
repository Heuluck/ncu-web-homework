package com.ncu.chat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupConversationVO {
    private Long groupId;
    private String groupName;
    private String groupAvatar;
    private String lastMessage;
    private String lastMessageType;
    private LocalDateTime lastTime;
    private Integer unreadCount;
    private String announcement;
}