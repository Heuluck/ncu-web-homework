package com.ncu.chat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long friendId;
    private String nickname;
    private String avatar;
    private Integer onlineStatus;     // 0-离线 1-在线 2-忙碌 3-勿扰
    private String lastMessage;       // 最后一条消息摘要
    private String lastMessageType;   // 最后消息类型文本
    private LocalDateTime lastTime;   // 最后消息时间
    private Integer unreadCount;      // 未读消息数
}
